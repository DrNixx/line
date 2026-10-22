package lila.oidc

import play.api.libs.json.*

/** Platforms that may authenticate with a signed payload brokered by the IdP. */
enum PlatformProvider(val value: String, val subjectTokenType: String):
  case YandexGame
      extends PlatformProvider(
        "yandex-game",
        "urn:chess-online:params:token-type:yandex-game-signature"
      )
  case VkGame
      extends PlatformProvider(
        "vk-game",
        "urn:chess-online:params:token-type:vk-game-signature"
      )
  case FridayGame
      extends PlatformProvider(
        "friday-game",
        "urn:chess-online:params:token-type:friday-game-signature"
      )

object PlatformProvider:

  val all: List[PlatformProvider] = values.toList

  def from(value: String): Option[PlatformProvider] = all.find(_.value == value)

enum SignatureError:
  case InvalidRequest
  case UnknownProvider
  case InvalidSignature
  case AccessDenied
  case RateLimited
  case Unavailable
  case ProviderError

case class SignatureRequest(provider: String, signature: String)

object SignatureRequest:

  def fromJson(json: JsValue): Either[SignatureError, SignatureRequest] =
    json
      .asOpt[JsObject]
      .toRight(SignatureError.InvalidRequest)
      .flatMap: obj =>
        for
          provider <- (obj \ "provider")
            .asOpt[String]
            .map(_.trim)
            .filter(_.nonEmpty)
            .toRight(SignatureError.InvalidRequest)
          signature <- (obj \ "signature").asOpt[String].toRight(SignatureError.InvalidRequest)
        yield SignatureRequest(provider, signature)

object PlatformSignature:

  /** Platform assertions are short; reject oversized input before it reaches the IdP. */
  val MaxSignatureLength = 8192

  def validate(
      provider: String,
      signature: String,
      allowed: Set[PlatformProvider]
  ): Either[SignatureError, (PlatformProvider, String)] =
    PlatformProvider.from(provider) match
      case Some(p) if allowed.contains(p) =>
        val trimmed = signature.trim
        if trimmed.isEmpty || trimmed.length > MaxSignatureLength then Left(SignatureError.InvalidRequest)
        else Right(p -> trimmed)
      case _ => Left(SignatureError.UnknownProvider)

  def validateJson(
      json: JsValue,
      allowed: Set[PlatformProvider]
  ): Either[SignatureError, (PlatformProvider, String)] =
    SignatureRequest
      .fromJson(json)
      .flatMap: request =>
        validate(request.provider, request.signature, allowed)

/** Exchanges a platform signature for the canonical identity returned by the IdP. */
trait PlatformSignatureBroker:

  def exchange(
      provider: PlatformProvider,
      signature: String
  ): Fu[Either[SignatureError, ExternalIdentity]]
