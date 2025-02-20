package lila.oidc

import java.net.URI

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.{ ClientID, Issuer }

case class OIDC(
    issuer: Issuer,
    clientID: ClientID,
    clientSecret: Secret,
    signinCallbackUrl: URI,
    jwsAlgorithm: Option[JWSAlgorithm]
)

object OIDCFactory {
  def apply(issuer: String, clientID: String, clientSecret: String, signinCallbackUrl: String, jwsAlgorithm: Option[String]): OIDC =
    new OIDC(new Issuer(issuer), new ClientID(clientID), new Secret(clientSecret), new URI(signinCallbackUrl), jwsAlgorithm.map(JWSAlgorithm.parse))
}
