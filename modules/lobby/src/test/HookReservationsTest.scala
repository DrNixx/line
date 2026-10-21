package lila.lobby

import scala.concurrent.duration.*

class HookReservationsTest extends munit.FunSuite:

  test("reserve and release"):
    val r = new HookReservations(30.seconds)
    assert(!r.isReserved("a"))
    r.reserve(List("a", "b"))
    assert(r.isReserved("a"))
    assert(r.isReserved("b"))
    r.release(List("a"))
    assert(!r.isReserved("a"))
    assert(r.isReserved("b"))

  test("releasing unknown ids is safe"):
    val r = new HookReservations(30.seconds)
    r.release(List("x"))
    assert(!r.isReserved("x"))
