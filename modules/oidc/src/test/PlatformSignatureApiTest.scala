package lila.oidc

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet

import lila.common.EmailAddress

class PlatformSignatureApiTest extends munit.FunSuite:

  private val issuer = "https://passport.chess-online.com"

  private val audience = "lichess"

  private def claims(json: String): Either[SignatureError, ExternalIdentity] =
    PlatformSignatureApi.toIdentity(JWTClaimsSet.parse(json), audience)

  test("token exchange request follows RFC 8693") {
    val params = PlatformSignatureApi.requestParams(
      provider = PlatformProvider.YandexGame,
      signature = "sig.payload",
      audience = "lichess",
      scopes = Set("openid", "profile", "email")
    )
    assertEquals(params("grant_type"), Seq(PlatformSignatureApi.TokenExchangeGrantType))
    assertEquals(params("subject_token"), Seq("sig.payload"))
    assertEquals(
      params("subject_token_type"),
      Seq("urn:chess-online:params:token-type:yandex-game-signature")
    )
    assertEquals(params("requested_token_type"), Seq(PlatformSignatureApi.IdTokenType))
    assertEquals(params("audience"), Seq("lichess"))
    assertEquals(params("scope"), Seq("email openid profile"))
  }

  test("all providers have a distinct subject token type") {
    val types = PlatformProvider.all.map(_.subjectTokenType)
    assertEquals(types.distinct.size, types.size)
    assert(types.forall(_.startsWith("urn:chess-online:params:token-type:")))
  }

  test("exchanged ID token is read from access_token") {
    assertEquals(
      PlatformSignatureApi.parseTokenResponse(
        200,
        """{"access_token":"header.payload.signature",
          |"issued_token_type":"urn:ietf:params:oauth:token-type:id_token",
          |"token_type":"N/A","expires_in":60}""".stripMargin
      ),
      Right("header.payload.signature")
    )
  }

  test("token response requires the exact issued token type") {
    assertEquals(
      PlatformSignatureApi.parseTokenResponse(
        200,
        """{"access_token":"jwt","issued_token_type":"urn:ietf:params:oauth:token-type:access_token"}"""
      ),
      Left(SignatureError.ProviderError)
    )
    assertEquals(
      PlatformSignatureApi.parseTokenResponse(200, """{"access_token":"jwt"}"""),
      Left(SignatureError.ProviderError)
    )
    assertEquals(
      PlatformSignatureApi
        .parseTokenResponse(200, """{"issued_token_type":"urn:ietf:params:oauth:token-type:id_token"}"""),
      Left(SignatureError.ProviderError)
    )
    assertEquals(
      PlatformSignatureApi.parseTokenResponse(200, "not json"),
      Left(SignatureError.ProviderError)
    )
  }

  test("OAuth2 error responses are mapped") {
    def error(status: Int, code: String) =
      PlatformSignatureApi.parseTokenResponse(status, s"""{"error":"$code"}""")

    assertEquals(error(400, "invalid_grant"), Left(SignatureError.InvalidSignature))
    assertEquals(error(400, "access_denied"), Left(SignatureError.AccessDenied))
    assertEquals(error(400, "invalid_target"), Left(SignatureError.ProviderError))
    assertEquals(error(400, "invalid_scope"), Left(SignatureError.ProviderError))
    assertEquals(error(401, "invalid_client"), Left(SignatureError.Unavailable))
    assertEquals(error(429, "temporarily_unavailable"), Left(SignatureError.RateLimited))
    assertEquals(error(503, "temporarily_unavailable"), Left(SignatureError.Unavailable))
    assertEquals(PlatformSignatureApi.parseTokenResponse(500, ""), Left(SignatureError.Unavailable))
  }

  test("ID token claims map to an external identity") {
    assertEquals(
      claims(
        s"""{"iss":"$issuer","sub":"1001","aud":"lichess","iat":1764319157,"exp":1764319217,
           |"preferred_username":"Nick","email":"nick@example.com",
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
      claims(
        s"""{"iss":"$issuer","sub":"1001","aud":"lichess","preferred_username":"Nick"}"""
      ),
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
      claims(
        s"""{"iss":"$issuer","sub":"1001","aud":"lichess","preferred_username":"Nick",
           |"email":"not-an-email","email_verified":true}""".stripMargin
      ).map(_.email),
      Right(None)
    )
  }

  test("exchanged ID token must be short-lived") {
    def lifetime(iat: Long, exp: Long): Boolean =
      PlatformSignatureApi.validLifetime(
        new IDTokenClaimsSet(
          JWTClaimsSet.parse(
            s"""{"iss":"$issuer","sub":"1001","aud":"lichess","iat":$iat,"exp":$exp}"""
          )
        )
      )

    assert(lifetime(1764319157L, 1764319217L))
    assert(!lifetime(1764319157L, 1764319457L))
    assert(!lifetime(1764319157L, 1764319157L))
  }

  test("foreign audience is rejected") {
    assertEquals(
      PlatformSignatureApi.toIdentity(
        JWTClaimsSet.parse(s"""{"iss":"$issuer","sub":"1001","aud":"other-client",
                              |"preferred_username":"Nick"}""".stripMargin),
        audience
      ),
      Left(SignatureError.ProviderError)
    )
  }

  test("missing sub or username is a provider error") {
    assertEquals(
      claims(s"""{"iss":"$issuer","aud":"lichess","preferred_username":"Nick"}"""),
      Left(SignatureError.ProviderError)
    )
    assertEquals(
      claims(s"""{"iss":"$issuer","sub":"1001","aud":"lichess"}"""),
      Left(SignatureError.ProviderError)
    )
    assertEquals(
      claims(s"""{"iss":"$issuer","sub":"  ","aud":"lichess","preferred_username":"Nick"}"""),
      Left(SignatureError.ProviderError)
    )
  }
