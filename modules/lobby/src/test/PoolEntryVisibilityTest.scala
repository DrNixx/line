package lila.lobby

import chess.IntRating

import lila.core.pool.{ Blocking, PoolMember }
import lila.core.rating.RatingRange
import lila.core.socket.Sri
import lila.core.userId.{ UserId, UserName }
import lila.rating.{ PerfKey, PerfType }

final class PoolEntryVisibilityTest extends munit.FunSuite:

  private val viewerId = UserId("viewer")
  private val targetId = UserId("target")
  private val perf     = PerfType(PerfKey.blitz)

  private def viewer(
      lame: Boolean = false,
      bot: Boolean = false,
      blocking: Set[UserId] = Set.empty
  ) = LobbyUser(
    id = viewerId,
    username = UserName("viewer"),
    lame = lame,
    bot = bot,
    perfMap = Map.empty,
    blocking = Blocking(blocking)
  )

  private def entry(
      userId: UserId = targetId,
      lame: Boolean = false,
      blocking: Set[UserId] = Set.empty,
      ratingRange: Option[RatingRange] = None,
      rating: IntRating = IntRating(1500)
  ) = PoolMember(
    userId = userId,
    username = UserName("target"),
    sri = Sri("sri"),
    rating = rating,
    displayRating = rating,
    provisional = false,
    ratingRange = ratingRange,
    lame = lame,
    blocking = Blocking(blocking),
    rageSitCounter = 0
  )

  test("visible when compatible and not blocked"):
    assert(LobbySocket.poolEntryVisibleTo(viewer(), entry(), perf))

  test("hidden for self"):
    assert(!LobbySocket.poolEntryVisibleTo(viewer(), entry(userId = viewerId), perf))

  test("hidden when viewer blocks target"):
    assert(!LobbySocket.poolEntryVisibleTo(viewer(blocking = Set(targetId)), entry(), perf))

  test("hidden when target blocks viewer"):
    assert(!LobbySocket.poolEntryVisibleTo(viewer(), entry(blocking = Set(viewerId)), perf))

  test("hidden when lame differs"):
    assert(!LobbySocket.poolEntryVisibleTo(viewer(), entry(lame = true), perf))

  test("hidden when target rating range excludes viewer"):
    assert(
      !LobbySocket.poolEntryVisibleTo(
        viewer(),
        entry(ratingRange = Some(RatingRange(IntRating(1600), IntRating(1800)))),
        perf
      )
    )

  test("visible when target rating range includes viewer"):
    assert(
      LobbySocket.poolEntryVisibleTo(
        viewer(),
        entry(ratingRange = Some(RatingRange(IntRating(1400), IntRating(1600)))),
        perf
      )
    )

  test("hidden for bot viewer"):
    assert(!LobbySocket.poolEntryVisibleTo(viewer(bot = true), entry(), perf))
