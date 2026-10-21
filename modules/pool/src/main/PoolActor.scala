package lila.pool

import akka.actor.*
import akka.pattern.pipe
import scalalib.ThreadLocalRandom

import lila.core.pool.{ HookThieve, Joiner, PoolMember }
import lila.core.socket.Sris

final private class PoolActor(
    config: PoolConfig,
    hookThieve: HookThieve,
    gameStarter: GameStarter
) extends Actor:

  import PoolActor.*

  var members = Vector.empty[PoolMember]

  private var mergeMode = false // Шаг 1: пока без изменения матчинга, используется в Шаге 4

  private var lastPairedUserIds = Set.empty[UserId]

  var nextWave: Cancellable = scala.compiletime.uninitialized

  given Executor = context.dispatcher

  private def publishMembers(): Unit =
    if mergeMode then
      lila.common.Bus.publish(
        lila.core.pool.PoolMembers(config.id, config.clock, members),
        "poolMembers"
      )
  def scheduleWave() =
    nextWave = context.system.scheduler.scheduleOnce(
      config.wave.every + ThreadLocalRandom.nextInt(1000).millis,
      self,
      ScheduledWave
    )

  scheduleWave()

  def receive =

    case Join(joiner, _) if lastPairedUserIds(joiner.me) =>
    // don't pair someone twice in a row, it's probably a client error

    case Join(joiner, rageSit) =>
      members.find(joiner.is(_)) match
        case None =>
          members = members :+ lila.pool.PoolMember(joiner, rageSit)
          publishMembers()
          val needed = if mergeMode then 2 else config.wave.players.value
          if members.sizeIs >= needed then self ! FullWave
        case Some(member) if member.ratingRange != joiner.ratingRange =>
          members = members.map: m =>
            if m == member then m.withRange(joiner.ratingRange) else m
          publishMembers()
        case _ => // no change
    case Leave(userId) =>
      val before = members
      members = members.filterNot(_.userId == userId)
      if members != before then publishMembers()

    case SetMergeMode(enabled) =>
      mergeMode = enabled
      if enabled then
        publishMembers()
        if members.sizeIs >= 2 then self ! FullWave

    case ScheduledWave =>
      monitor.scheduled(monId).increment()
      self ! RunWave

    case FullWave =>
      monitor.full(monId).increment()
      self ! RunWave

    case RunWave =>
      nextWave.cancel()
      hookThieve.candidates(config.clock).pipeTo(self)
      ()

    case HookThieve.PoolHooks(hooks) =>
      monitor.withRange(monId).record(members.count(_.hasRange))

      val candidates = members ++ hooks.map(_.member)

      val pairings = MatchMaking(candidates)

      val pairedMembers = pairings.flatMap(_.members)

      hookThieve.stolen(
        hooks.filter: h =>
          pairedMembers.exists(h.is(_)),
        monId
      )

      members = members.diff(pairedMembers).map(_.incMisses)
      publishMembers()

      if pairings.nonEmpty then gameStarter(config, pairings)

      monitor.candidates(monId).record(candidates.size)
      monitor.paired(monId).record(pairedMembers.size)
      monitor.missed(monId).record(members.size)
      pairings.foreach: p =>
        monitor.ratingDiff(monId).record(p.ratingDiff.value)

      lastPairedUserIds = pairedMembers.view.map(_.userId).toSet

      scheduleWave()

    case Sris(sris) =>
      val before = members
      members = members.filter: m =>
        sris contains m.sri
      if members != before then publishMembers()

  val monitor = lila.mon.lobby.pool.wave
  val monId   = config.id.value.replace('+', '_')

private object PoolActor:

  case class Join(joiner: Joiner, rageSit: lila.core.playban.RageSit)
  case class Leave(userId: UserId) extends AnyVal
  case class SetMergeMode(enabled: Boolean)

  case object ScheduledWave
  case object FullWave
  case object RunWave
