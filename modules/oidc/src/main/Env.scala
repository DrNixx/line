package lila.oidc

import com.softwaremill.macwire.*
import play.api.ConfigLoader
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

import lila.common.autoconfig.{ *, given }
import lila.common.config.{ *, given }
import lila.core.config.*

@Module
private case class OidcConfig(
    @ConfigName("issuer.base_url") val issuer: String,
    @ConfigName("issuer.client_id") val clientId: String,
    @ConfigName("issuer.client_secret") val clientSecret: Secret,
    @ConfigName("issuer.scopes") val scopes: String,
    @ConfigName("issuer.jws_alg") val jwsAlgorithm: String,
    @ConfigName("issuer.token_auth_method") val tokenAuthMethod: String,
    @ConfigName("signature") val signature: SignatureConfig
)

@Module
private case class SignatureConfig(
    enabled: Boolean,
    @ConfigName("exchange_url") val exchangeUrl: String,
    providers: String,
    timeout: FiniteDuration
)

private object SignatureConfig:
  given ConfigLoader[SignatureConfig] = AutoConfig.loader

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    userRepo: lila.user.UserRepo,
    identity: lila.identity.Env,
    db: lila.db.Db,
    mode: play.api.Mode
)(using Executor, akka.stream.Materializer):
  import net.baseUrl

  private val config = appConfig.get[OidcConfig]("oidc")(AutoConfig.loader)

  if mode.isProd then
    if config.clientId.isEmpty then
      throw new IllegalArgumentException("oidc.issuer.client_id must be configured in production")
    if config.clientSecret.value.isEmpty then
      throw new IllegalArgumentException("oidc.issuer.client_secret must be configured in production")

  private val signatureProviders: Set[PlatformProvider] =
    config.signature.providers
      .split("\\s+")
      .filter(_.nonEmpty)
      .map: value =>
        PlatformProvider
          .from(value)
          .getOrElse:
            throw new IllegalArgumentException(s"oidc.signature.providers: unknown provider '$value'")
      .toSet

  if config.signature.enabled then
    if config.signature.exchangeUrl.trim.isEmpty then
      throw new IllegalArgumentException(
        "oidc.signature.exchange_url is required when signature login is enabled"
      )
    if signatureProviders.isEmpty then
      throw new IllegalArgumentException("oidc.signature.providers must not be empty")
    if config.signature.timeout <= scala.concurrent.duration.Duration.Zero then
      throw new IllegalArgumentException("oidc.signature.timeout must be positive")

  def signatureEnabled: Boolean                        = config.signature.enabled
  def allowedSignatureProviders: Set[PlatformProvider] = signatureProviders
  def signatureTimeout: FiniteDuration                 = config.signature.timeout
  def signatureExchangeUrl: String                     = config.signature.exchangeUrl

  private lazy val oidc = OIDCFactory.apply(
    config.issuer,
    config.clientId,
    config.clientSecret.value,
    s"$baseUrl/signin-oidc",
    config.jwsAlgorithm,
    config.scopes,
    config.tokenAuthMethod
  )

  private lazy val provisioning = UserProvisioning(userRepo, identity.statusRepo, identity.aliasRepo)

  private lazy val stateRepo = OidcStateRepo(db(lila.core.config.CollName("oidc_state")))

  lazy val api = OidcApi(oidc, provisioning, stateRepo)
