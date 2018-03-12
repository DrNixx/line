package lila.security

import org.pac4j.oidc.client.OidcClient

import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.profile.OidcProfile

final class Oidc(providerUrl: String, clientId: String, clientSecret: String, scope: String) {

  def client: OidcClient[OidcProfile] = {
    val oidcConfiguration = new OidcConfiguration()
    oidcConfiguration.setClientId(clientId)
    oidcConfiguration.setSecret(clientSecret)
    oidcConfiguration.setDiscoveryURI(s"$providerUrl/.well-known/openid-configuration")
    oidcConfiguration.setScope(s"openid $scope");
    val oidcClient = new OidcClient[OidcProfile](oidcConfiguration)
    oidcClient
  }
}