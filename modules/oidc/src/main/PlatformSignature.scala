package lila.oidc

/** Platforms that may authenticate with a signed payload brokered by the IdP. */
enum PlatformProvider(val value: String):
  case YandexGame extends PlatformProvider("yandex-game")
  case VkGame     extends PlatformProvider("vk-game")
  case FridayGame extends PlatformProvider("friday-game")

object PlatformProvider:

  val all: List[PlatformProvider] = values.toList

  def from(value: String): Option[PlatformProvider] = all.find(_.value == value)

enum SignatureError:
  case InvalidRequest
  case UnknownProvider
  case InvalidSignature
  case Unavailable
  case ProviderError

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

/** Exchanges a platform signature for the canonical identity returned by the IdP. */
trait PlatformSignatureBroker:

  def exchange(
      provider: PlatformProvider,
      signature: String
  ): Fu[Either[SignatureError, ExternalIdentity]]
