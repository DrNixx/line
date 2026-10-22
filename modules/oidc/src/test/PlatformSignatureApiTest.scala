package lila.oidc

import lila.common.EmailAddress

class PlatformSignatureApiTest extends munit.FunSuite:

  private val issuer = "https://passport.chess-online.com"

  private def claims(json: String): Either[SignatureError, ExternalIdentity] =
    PlatformSignatureApi.decode(issuer, 200, json)

  test("full claims are decoded") {
    assertEquals(
      claims(
        """{"sub":"1001","preferred_username":"Nick","email":"nick@example.com",
          |"email_verified":true,"globally_enabled":false}""".stripMargin
      ),
      Right(
        ExternalIdentity(
          issuer = issuer,
          subject = "1001",
          username = "Nick",
          email = EmailAddress.from("nick@example.com"),
          emailVerified = true,
          globallyEnabled = Some(false)
        )
      )
    )
  }

  test("optional claims default safely") {
    assertEquals(
      claims("""{"sub":"1001","preferred_username":"Nick"}"""),
      Right(
        ExternalIdentity(
          issuer = issuer,
          subject = "1001",
          username = "Nick",
          email = None,
          emailVerified = false,
          globallyEnabled = None
        )
      )
    )
  }

  test("invalid email is ignored, not fatal") {
    assertEquals(
      claims("""{"sub":"1001","preferred_username":"Nick","email":"not-an-email","email_verified":true}""")
        .map(_.email),
      Right(None)
    )
  }

  test("missing sub or username is a provider error") {
    assertEquals(claims("""{"preferred_username":"Nick"}"""), Left(SignatureError.ProviderError))
    assertEquals(claims("""{"sub":"1001"}"""), Left(SignatureError.ProviderError))
    assertEquals(claims("""{"sub":"  ","preferred_username":"Nick"}"""), Left(SignatureError.ProviderError))
  }

  test("invalid JSON and oversized body are provider errors") {
    assertEquals(PlatformSignatureApi.decode(issuer, 200, "not json"), Left(SignatureError.ProviderError))
    assertEquals(
      PlatformSignatureApi.decode(issuer, 200, "x" * (16 * 1024 + 1)),
      Left(SignatureError.ProviderError)
    )
  }

  test("IdP statuses are mapped") {
    assertEquals(PlatformSignatureApi.decode(issuer, 400, ""), Left(SignatureError.InvalidSignature))
    assertEquals(PlatformSignatureApi.decode(issuer, 403, ""), Left(SignatureError.InvalidSignature))
    assertEquals(PlatformSignatureApi.decode(issuer, 401, ""), Left(SignatureError.Unavailable))
    assertEquals(PlatformSignatureApi.decode(issuer, 500, ""), Left(SignatureError.Unavailable))
  }
