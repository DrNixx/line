package lila.oidc

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*
import scala.util.Try

import play.api.libs.json.*
import play.api.libs.ws.{ StandaloneWSClient, WSAuthScheme }
import play.api.libs.ws.DefaultBodyWritables.*

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.openid.connect.sdk.claims.{ IDTokenClaimsSet, UserInfo }

import lila.common.EmailAddress

final class PlatformSignatureApi(
    ws: StandaloneWSClient,
    service: OpenIDConnectService,
    timeout: FiniteDuration
)(using Executor)
    extends PlatformSignatureBroker:

  import PlatformSignatureApi.*

  private val logger = lila.log("oidc")

  def exchange(
      provider: PlatformProvider,
      signature: String
  ): Fu[Either[SignatureError, ExternalIdentity]] =
    service.tokenEndpoint
      .flatMap: endpoint =>
        ws.url(endpoint.toString)
          .withRequestTimeout(timeout)
          .withAuth(
            service.oidc.clientID.getValue,
            service.oidc.clientSecret.getValue,
            WSAuthScheme.BASIC
          )
          .post:
            requestParams(
              provider = provider,
              signature = signature,
              audience = service.oidc.clientID.getValue,
              scopes = service.oidc.scopes
            )
          .flatMap: response =>
            parseTokenResponse(response.status, response.body) match
              case Left(error) =>
                logError(provider, response.status, error)
                fuccess(Left(error))
              case Right(idToken) =>
                service
                  .validateExchangedIdToken(idToken)
                  .map: claims =>
                    if validLifetime(claims) then
                      toIdentity(claims.toJWTClaimsSet, service.oidc.clientID.getValue)
                    else Left(SignatureError.ProviderError)
                  .recover: e =>
                    logger.warn(
                      s"platform ID token rejected provider=${provider.value} ${e.getClass.getSimpleName}"
                    )
                    Left(SignatureError.ProviderError)
      .recover: e =>
        logger.warn(
          s"platform token exchange unavailable provider=${provider.value} ${e.getClass.getSimpleName}"
        )
        Left(SignatureError.Unavailable)

  private def logError(
      provider: PlatformProvider,
      status: Int,
      error: SignatureError
  ): Unit =
    error match
      case SignatureError.InvalidSignature =>
        logger.info(s"platform signature rejected provider=${provider.value}")
      case SignatureError.AccessDenied =>
        logger.info(s"platform account denied provider=${provider.value}")
      case _ =>
        logger.warn(
          s"platform token exchange failed provider=${provider.value} status=$status error=$error"
        )

private[oidc] object PlatformSignatureApi:

  val IdTokenType            = "urn:ietf:params:oauth:token-type:id_token"
  val TokenExchangeGrantType = "urn:ietf:params:oauth:grant-type:token-exchange"

  def requestParams(
      provider: PlatformProvider,
      signature: String,
      audience: String,
      scopes: Set[String]
  ): Map[String, Seq[String]] =
    Map(
      "grant_type"           -> Seq(TokenExchangeGrantType),
      "subject_token"        -> Seq(signature),
      "subject_token_type"   -> Seq(provider.subjectTokenType),
      "requested_token_type" -> Seq(IdTokenType),
      "audience"             -> Seq(audience),
      "scope"                -> Seq(scopes.toSeq.sorted.mkString(" "))
    )

  /** Parse the RFC 8693 response. The issued ID token travels in `access_token`. */
  def parseTokenResponse(status: Int, body: String): Either[SignatureError, String] =
    status match
      case 200 =>
        Try(Json.parse(body)).toOption
          .flatMap(_.asOpt[JsObject])
          .toRight(SignatureError.ProviderError)
          .flatMap: obj =>
            for
              issued <- (obj \ "issued_token_type")
                .asOpt[String]
                .filter(_ == IdTokenType)
                .toRight(SignatureError.ProviderError)
              token <- (obj \ "access_token")
                .asOpt[String]
                .map(_.trim)
                .filter(_.nonEmpty)
                .toRight(SignatureError.ProviderError)
            yield token
      case 400 =>
        errorCode(body) match
          case Some("invalid_grant") => Left(SignatureError.InvalidSignature)
          case Some("access_denied") => Left(SignatureError.AccessDenied)
          case _                     => Left(SignatureError.ProviderError)
      case 429 => Left(SignatureError.RateLimited)
      case _   => Left(SignatureError.Unavailable)

  /** The exchanged ID token must be short-lived; passport issues 60 seconds, 120 is the hard limit. */
  def validLifetime(claims: IDTokenClaimsSet, maxSeconds: Long = 120): Boolean =
    Option(claims.getIssueTime)
      .zip(Option(claims.getExpirationTime))
      .exists: (iat, exp) =>
        val seconds = (exp.getTime - iat.getTime) / 1000
        seconds > 0 && seconds <= maxSeconds

  def toIdentity(claims: JWTClaimsSet, audience: String): Either[SignatureError, ExternalIdentity] =
    if claims.getAudience.asScala.toList != List(audience) then Left(SignatureError.ProviderError)
    else
      Try:
        val userInfo = new UserInfo(claims)
        for
          subject <- Option(claims.getSubject)
            .map(_.trim)
            .filter(_.nonEmpty)
            .toRight(SignatureError.ProviderError)
          username <- Option(userInfo.getPreferredUsername)
            .map(_.trim)
            .filter(_.nonEmpty)
            .toRight(SignatureError.ProviderError)
        yield ExternalIdentity(
          issuer = Option(claims.getIssuer).getOrElse(""),
          subject = subject,
          username = username,
          email = Option(userInfo.getEmailAddress).flatMap(EmailAddress.from),
          emailVerified = Option(userInfo.getEmailVerified).exists(_.booleanValue),
          globallyEnabled = Option(userInfo.getClaim("globally_enabled")).flatMap:
            case b: java.lang.Boolean => Some(b.booleanValue)
            case _                    => None
        )
      .toOption
        .flatMap(_.toOption)
        .toRight(SignatureError.ProviderError)

  private def errorCode(body: String): Option[String] =
    Try(Json.parse(body)).toOption
      .flatMap(_.asOpt[JsObject])
      .flatMap(obj => (obj \ "error").asOpt[String])
