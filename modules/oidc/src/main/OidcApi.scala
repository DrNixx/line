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

  def getAuthenticationRequest(): Fu[AuthenticationRequest] = {
    val client = new OpenIDConnectService(oidc);
    val authenticationRequest = client.createOIDCAuthenticationRequest()
    authenticationRequest
  }

  def authenticate(
    params: Map[String, String],
    state: String,
    nonce: String
  ) = {
    val client = new OpenIDConnectService(oidc);
    val user = client.authenticate(params, new State(state), new Nonce(nonce))

  }

}