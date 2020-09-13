package lila.oidc

import ornicar.scalalib.Random
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.Nonce
import com.nimbusds.openid.connect.sdk.claims.UserInfo
import lila.common.EmailAddress
import lila.user.{Authenticator, User, UserRepo}
import User.ClearPassword
import play.api.mvc.RequestHeader

final class OidcApi(
    oidc: OIDC,
    userRepo: UserRepo,
    authenticator: Authenticator
)(implicit ec: scala.concurrent.ExecutionContext) {

  def getAuthenticationRequest(): Fu[AuthenticationRequest] = {
    val client = new OpenIDConnectService(oidc);
    val authenticationRequest = client.createOIDCAuthenticationRequest()
    authenticationRequest
  }

  def authenticate(req: RequestHeader): Fu[Option[User]] = {
    val redirectBackURI = req.flash("referrer")
    val state = req.flash("oidcState")
    val nonce = req.flash("oidcNonce")
    val params = req.queryString.map { case (k, Seq(v)) => (k, v) }
    val client = new OpenIDConnectService(oidc);

    client.authenticate(params, new State(state), new Nonce(nonce)) flatMap { userInfo =>
      getOrCreateUser(userInfo)
    }
  }

  private def getOrCreateUser(userInfo: UserInfo): Fu[Option[User]] = {
    userRepo byId userInfo.getSubject.toString flatMap {
      case Some(user) =>
        userRepo setUsername (user.id, userInfo.getPreferredUsername)
        fuccess(Some(user))
      case None =>
        createOidcUser(userInfo)
    }
  }

  private def createOidcUser(userInfo: UserInfo): Fu[Option[User]] = {
    val pwd = authenticator passEnc ClearPassword(Random secureString 12)
    val blind = false
    val confirmEmail = false
    val email = new EmailAddress(userInfo.getEmailAddress)
    var username = userInfo.getPreferredUsername
    userRepo.create2(userInfo.getSubject.toString, username, pwd, email, blind, none, confirmEmail)
  }
}
