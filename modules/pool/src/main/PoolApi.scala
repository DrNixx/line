package lila.pool

import akka.actor.*

import lila.game.Game
import lila.rating.PerfType
import lila.core.rating.{ RatingRange, PerfKey }
import lila.core.socket.{ Sri, Sris }
import lila.core.pool.{ PoolMember, PoolConfigId, Joiner }
import lila.user.Me

final class PoolApi(
    val configs: List[PoolConfig],
    hookThieve: HookThieve,
    gameStarter: GameStarter,
    hasCurrentBan: lila.core.playban.HasCurrentBan,
    rageSitOf: lila.core.playban.RageSitOf,
    system: ActorSystem
)(using Executor)
    extends lila.core.pool.PoolApi:
  import PoolActor.*

  private val actors: Map[PoolConfigId, ActorRef] = configs
    .map: config =>
      config.id -> system
        .actorOf(
          Props(PoolActor(config, hookThieve, gameStarter)),
          name = s"pool-${config.id}"
        )
    .toMap

  val poolPerfKeys: Map[PoolConfigId, PerfKey] = configs
    .map: config =>
      config.id -> config.perfType.key
    .toMap

  def join(poolId: PoolConfigId, joiner: Joiner): Unit =
    hasCurrentBan(joiner.me.id)
      .foreach:
        case false =>
          actors.foreach:
            case (id, actor) if id == poolId =>
              rageSitOf(joiner.me.id).foreach(actor ! Join(joiner, _))
            case (_, actor) => actor ! Leave(joiner.me)
        case _ =>

  def leave(poolId: PoolConfigId, userId: UserId) = sendTo(poolId, Leave(userId))

  def setOnlineSris(ids: Sris): Unit = actors.values.foreach(_ ! ids)

  private def sendTo(poolId: PoolConfigId, msg: Any) =
    actors.get(poolId).foreach { _ ! msg }
