package lila.oidc

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    authenticator: lila.user.Authenticator,
    asyncCache: lila.memo.AsyncCache.Builder,
    settingStore: lila.memo.SettingStore.Builder,
    db: lila.db.Env,
    lifecycle: play.api.inject.ApplicationLifecycle
) {
  private val settings = new {
    val OidcIssuer = config getString "oidc.issuer"
    val OidcClientId = config getString "oidc.client_id"
    val OidcClientSecret = config getString "oidc.client_secret"
    val OidcScopes = config getString "oidc.scopes"
    val OidcJwsAlgorithm = config getString "oidc.jws_alg"
    val NetBaseUrl = config getString "net.base_url"
    val NetDomain = config getString "net.domain"
    val NetEmail = config getString "net.email"
  }
  import settings._

  lazy val oidc = OIDCFactory.apply(
    OidcIssuer,
    OidcClientId,
    OidcClientSecret,
    s"$NetBaseUrl/signin-oidc",
    Some(OidcJwsAlgorithm)
  )

  // oidc
  /*
  val oidc = new Oidc(
    providerUrl = config getString "passport.url",
    clientId = config getString "passport.client",
    clientSecret = config getString "passport.secret",
    scope = config getString "passport.scope",
    baseUrl = NetBaseUrl
  )

  val oidcConfiguration = new OidcConfiguration()
  oidcConfiguration.setClientId(clientId)
  oidcConfiguration.setSecret(clientSecret)
  oidcConfiguration.setDiscoveryURI(s"$providerUrl/.well-known/openid-configuration")
  oidcConfiguration.setScope(s"openid $scope");
  val oidcClient = new OidcClient[OidcProfile](oidcConfiguration)

  val clients = new Clients(baseUrl + "/callback", oidcClient)

  // callback
  val callbackController = new CallbackController()
  callbackController.setDefaultUrl("/")
  callbackController.setMultiProfile(true)
  bind(classOf[CallbackController]).toInstance(callbackController)

  // logout
  val logoutController = new ApplicationLogoutController()
  logoutController.setDefaultUrl("/")
  bind(classOf[ApplicationLogoutController]).toInstance(logoutController)
  */

  lazy val api = new OidcApi(NetBaseUrl, oidc, authenticator)
}

object Env {

  private lazy val system = lila.common.PlayApp.system

  lazy val current = "oidc" boot new Env(
    config = lila.common.PlayApp loadConfig "oidc",
    db = lila.db.Env.current,
    authenticator = lila.user.Env.current.authenticator,
    asyncCache = lila.memo.Env.current.asyncCache,
    settingStore = lila.memo.Env.current.settingStore,
    system = system,
    lifecycle = lila.common.PlayApp.lifecycle
  )
}
