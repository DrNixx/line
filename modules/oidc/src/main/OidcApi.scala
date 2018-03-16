package lila.oidc

import java.net.URI

import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.Nonce

import lila.common.{ LilaCookie }
import lila.user.{ User, UserRepo }

final class OidcApi(
    baseUrl: String,
    oidc: OIDC,
    authenticator: lila.user.Authenticator
) {

  def getAuthenticationRequest(): AuthenticationRequest = {
    val client = new OpenIDConnectService();
    val authenticationRequest = client.createOIDCAuthenticationRequest(oidc.issuer, oidc.clientID, oidc.signinCallbackUrl)
    authenticationRequest
  }
}