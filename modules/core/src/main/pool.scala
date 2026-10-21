package lila.core
package pool

import _root_.chess.{ ByColor, Clock }
import _root_.chess.IntRating
import alleycats.Zero

import lila.core.id.GameFullId
import lila.core.perf.PerfKey
import lila.core.rating.RatingRange
import lila.core.socket.Sri
import lila.core.userId.*

opaque type Blocking = Set[UserId]
object Blocking extends TotalWrapper[Blocking, Set[UserId]]:
  given Zero[Blocking] = Zero(Set.empty)

opaque type PoolConfigId = String
object PoolConfigId extends OpaqueString[PoolConfigId]

opaque type IsClockCompatible = Clock.Config => Boolean
object IsClockCompatible extends FunctionWrapper[IsClockCompatible, Clock.Config => Boolean]

case class PoolMember(
    userId: UserId,
    username: UserName,
    sri: Sri,
    rating: IntRating,
    displayRating: IntRating,
    provisional: Boolean,
    ratingRange: Option[RatingRange],
    lame: Boolean,
    blocking: Blocking,
    rageSitCounter: Int,
    misses: Int = 0 // how many waves they missed
)

object PoolMember:
  given UserIdOf[PoolMember] = _.userId

case class Pairing(players: ByColor[(Sri, GameFullId)])
case class Pairings(pairings: List[Pairing])

/** Internal Bus message. Contains blocking lists, must not be sent to clients as-is. */
case class PoolMembers(
    poolId: PoolConfigId,
    clock: Clock.Config,
    members: Vector[PoolMember]
)

object HookThieve:

  case class GetCandidates(clock: Clock.Config, promise: Promise[PoolHooks])
  case class StolenHookIds(ids: Vector[String])
  // Шаг 5.1: кандидаты не спарены — снимаем резерв в лобби (хуки остаются доступными)
  case class ReleasedHookIds(ids: Vector[String])

  case class PoolHook(hookId: String, member: PoolMember):
    def is(m: PoolMember) = member.userId == m.userId

  case class PoolHooks(hooks: Vector[PoolHook])

case class Joiner(
    sri: Sri,
    rating: IntRating,        // jittered, used for matchmaking
    displayRating: IntRating, // real rating, used for lobby display
    provisional: Boolean,
    ratingRange: Option[RatingRange],
    lame: Boolean,
    blocking: Blocking,
    username: UserName
)(using val me: MyId)

object Joiner:
  given UserIdOf[Joiner] = _.me.userId

trait PoolApi:
  def setOnlineSris(ids: socket.Sris): Unit
  def poolPerfKeys: Map[PoolConfigId, PerfKey]
  def join(poolId: PoolConfigId, joiner: Joiner): Unit
  def leave(poolId: PoolConfigId, user: UserId): Unit
  def setMergeMode(enabled: Boolean): Unit

  // Шаг 5.2: взаимоисключение — создание хука авторизованным владельцем выводит его из всех пулов
  def leaveAll(userId: UserId): Unit
