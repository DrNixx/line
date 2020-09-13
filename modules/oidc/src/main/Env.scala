package lila.oidc

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import lila.common.config._
import lila.user.{Authenticator, UserRepo}

private case class OidcConfig(
    @ConfigName("issuer.base_url") issuer: String,
    @ConfigName("issuer.client_id") clientId: String,
    @ConfigName("issuer.client_secret") clientSecret: Secret,
    @ConfigName("issuer.scopes") scopes: String,
    @ConfigName("issuer.jws_alg") jwsAlgorithm: String,
)

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    userRepo: UserRepo,
    authenticator: Authenticator
)(implicit ec: scala.concurrent.ExecutionContext) {
  import net.baseUrl

  private val config = appConfig.get[OidcConfig]("oidc")(AutoConfig.loader)

  private lazy val oidc = OIDCFactory.apply(
    config.issuer,
    config.clientId,
    config.clientSecret.value,
    s"$baseUrl/signin-oidc",
    Some(config.jwsAlgorithm)
  )

  lazy val api = wire[OidcApi]
}
