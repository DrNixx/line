package lila.user

class UserTest extends munit.FunSuite:

  given Conversion[String, UserStr] = UserStr(_)
  given Conversion[String, UserId]  = UserId(_)

  def canSignup(str: String) =
    import lila.user.nameRules.*
    newUsernameRegex.pattern.matcher(str).matches &&
    newUsernamePrefix.pattern.matcher(str).matches &&
    newUsernameSuffix.pattern.matcher(str).matches &&
    newUsernameChars.pattern.matcher(str).matches &&
    newUsernameLetters.pattern.matcher(str).matches

  import UserStr.couldBeUsername

  test("username regex bad prefix: can login") {
    assert(couldBeUsername("2"))
    // assert(couldBeUsername("0foo"))
    // assert(couldBeUsername("_foo"))
    // assert(couldBeUsername("__foo"))
    // assert(couldBeUsername("-foo"))
  }

  test("username regex bad prefix: cannot signup") {
    assert(!canSignup("000"))
    // assert(!canSignup("0foo"))
    // assert(!canSignup("_foo"))
    // assert(!canSignup("__foo"))
    // assert(!canSignup("-foo"))
  }
