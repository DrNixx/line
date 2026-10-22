package lila.oidc

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, WSAuthScheme }

import lila.common.EmailAddress
import lila.core.config.Secret

final class PlatformSignatureApi(
    ws: StandaloneWSClient,
    exchangeUrl: String,
    clientId: String,
    clientSecret: Secret,
    issuer: String,
    timeout: FiniteDuration
)(using Executor)
    extends PlatformSignatureBroker:

  import PlatformSignatureApi.*

  def exchange(
      provider: PlatformProvider,
      signature: String
  ): Fu[Either[SignatureError, ExternalIdentity]] =
    ws.url(exchangeUrl)
      .withRequestTimeout(timeout)
      .withAuth(clientId, clientSecret.value, WSAuthScheme.BASIC)
      .post(Json.obj("provider" -> provider.value, "signature" -> signature))
      .map: response =>
        val result = decode(issuer, response.status, response.body)
        result match
          case Right(_) => ()
          case Left(SignatureError.InvalidSignature) =>
            logger.info(s"platform signature rejected provider=${provider.value}")
          case Left(error) =>
            logger.warn(
              s"platform signature exchange failed provider=${provider.value} status=${response.status} error=$error"
            )
        result
      .recover { case e: Exception =>
        logger.warn(
          s"platform signature exchange unavailable provider=${provider.value} ${e.getClass.getSimpleName}"
        )
        Left(SignatureError.Unavailable)
      }

private[oidc] object PlatformSignatureApi:

  private val MaxResponseChars = 16 * 1024

  def decode(
      issuer: String,
      status: Int,
      body: String
  ): Either[SignatureError, ExternalIdentity] =
    status match
      case 200 =>
        if body.length > MaxResponseChars then Left(SignatureError.ProviderError)
        else
          Try(Json.parse(body)).toOption
            .flatMap(_.asOpt[JsObject])
            .toRight(SignatureError.ProviderError)
            .flatMap: obj =>
              for
                sub      <- requiredString(obj, "sub")
                username <- requiredString(obj, "preferred_username")
              yield ExternalIdentity(
                issuer = issuer,
                subject = sub,
                username = username,
                email = optionalEmail(obj),
                emailVerified = (obj \ "email_verified").asOpt[Boolean].getOrElse(false),
                globallyEnabled = (obj \ "globally_enabled").asOpt[Boolean]
              )
      case 400 | 403 => Left(SignatureError.InvalidSignature)
      case _         => Left(SignatureError.Unavailable)

  private def requiredString(obj: JsObject, field: String): Either[SignatureError, String] =
    (obj \ field).asOpt[String].map(_.trim).filter(_.nonEmpty).toRight(SignatureError.ProviderError)

  private def optionalEmail(obj: JsObject): Option[EmailAddress] =
    (obj \ "email").asOpt[String].map(_.trim).filter(_.nonEmpty).flatMap(EmailAddress.from)
