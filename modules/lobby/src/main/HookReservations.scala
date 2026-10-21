package lila.lobby

import scala.concurrent.duration.FiniteDuration

final private[lobby] class HookReservations(ttl: FiniteDuration):

  private var reserved = Map.empty[String, Instant]

  def reserve(ids: Iterable[String]): Unit =
    val now = nowInstant
    reserved = reserved ++ ids.map(_ -> now)

  def release(ids: Iterable[String]): Unit =
    reserved = reserved -- ids

  def isReserved(id: String): Boolean =
    reserved.contains(id)

  def cleanup(): Unit =
    val limit = nowInstant.minusMillis(ttl.toMillis)
    reserved = reserved.filter { case (_, at) => at.isAfter(limit) }
