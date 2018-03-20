package lila.oidc

import java.net.URI
import java.io.IOException

import com.nimbusds.jose.JWSAlgorithm.Family
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jose.{ JOSEException, JWSAlgorithm }
import com.nimbusds.oauth2.sdk._
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.{ ClientID, Issuer, State }
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator
import com.nimbusds.openid.connect.sdk.{ AuthenticationErrorResponse, _ }
import com.nimbusds.openid.connect.sdk.claims._;

import scala.collection.JavaConversions._

import lila.user.{ User, UserRepo }

// http://nemcio.cf/gitbucket/

final class OpenIDConnectService(
    val oidc: OIDC
) {

  lazy val metadata = getMetadata(oidc.issuer)

  private val JWK_REQUEST_TIMEOUT = 5000

  private val OIDC_SCOPE = new Scope(
    OIDCScopeValue.OPENID,
    OIDCScopeValue.EMAIL,
    OIDCScopeValue.PROFILE,
    OIDCScopeValue.ADDRESS
  )

  OIDC_SCOPE.add("profile_ex")

  /**
   * Get or create a user account federated with OIDC or SAML IdP.
   *
   * @param issuer            Issuer
   * @param subject           Subject
   * @param mailAddress       Mail address
   * @param preferredUserName Username (if this is none, username will be generated from the mail address)
   * @param fullName          Fullname (defaults to username)
   * @return User
   */
  def getOrCreateFederatedUser(
    issuer: String,
    subject: String,
    mailAddress: String,
    preferredUserName: Option[String],
    fullName: Option[String]
  ): Fu[User] = fufail("123")

  /**
   * Create an authentication request.
   *
   * @return Authentication request
   */
  def createOIDCAuthenticationRequest(): Fu[AuthenticationRequest] = metadata.flatMap { metadata =>
    fuccess(
      new AuthenticationRequest(
        metadata.getAuthorizationEndpointURI,
        new ResponseType(ResponseType.Value.CODE),
        OIDC_SCOPE,
        oidc.clientID,
        oidc.signinCallbackUrl,
        new State(),
        new Nonce()
      )
    )
  }

  /**
   * Proceed the OpenID Connect authentication.
   *
   * @param params      Query parameters of the authentication response
   * @param state       State saved in the session
   * @param nonce       Nonce saved in the session
   * @return User
   */
  def authenticate(
    params: Map[String, String],
    state: State,
    nonce: Nonce
  ): Fu[User] =
    validateOIDCAuthenticationResponse(params, state) flatMap { authenticationResponse =>
      obtainOIDCToken(authenticationResponse.getAuthorizationCode, nonce) flatMap { claims =>
        Seq("email", "preferred_username", "name").map(k => Option(claims.getStringClaim(k))) match {
          case Seq(Some(email), preferredUsername, name) =>
            logger.info(s"Claims: claims=${claims.toJSONObject}")
            getOrCreateFederatedUser(claims.getIssuer.getValue, claims.getSubject.getValue, email, preferredUsername, name)
          case _ =>
            fufail(s"OIDC ID token must have an email claim: claims=${claims.toJSONObject}")
        }
      }
    }

  /**
   * Validate the authentication response.
   *
   * @param params      Query parameters of the authentication response
   * @param state       State saved in the session
   * @return Authentication response
   */
  def validateOIDCAuthenticationResponse(params: Map[String, String], state: State): Fu[AuthenticationSuccessResponse] =
    try {
      AuthenticationResponseParser.parse(oidc.signinCallbackUrl, params) match {
        case response: AuthenticationSuccessResponse =>
          if (response.getState == state) {
            fuccess(response)
          } else {
            fufail(s"OIDC authentication state did not match: response(${response.getState}) != session($state)")
          }
        case response: AuthenticationErrorResponse =>
          fufail(s"OIDC authentication response has error: ${response.getErrorObject}")
      }
    } catch {
      case e: ParseException =>
        fufail(s"OIDC authentication response has error: $e")
    }

  /**
   * Obtain the ID token from the OpenID Provider.
   *
   * @param authorizationCode Authorization code in the query string
   * @param nonce             Nonce
   * @return Token response
   */
  def obtainOIDCToken(
    authorizationCode: AuthorizationCode,
    nonce: Nonce
  ): Fu[IDTokenClaimsSet] = metadata.flatMap { metadata =>
    sendOIDCTokenRequest(authorizationCode).flatMap { httpResponse =>
      try {
        OIDCTokenResponseParser.parse(httpResponse) match {
          case response: OIDCTokenResponse =>
            validateOIDCTokenResponse(response, nonce)
          case response: TokenErrorResponse =>
            fufail(s"OIDC token response has error: ${response.getErrorObject.toJSONObject}")
        }
      } catch {
        case e: ParseException =>
          fufail(s"OIDC token response has error: $e")
      }
    }
  }

  /**
   * Execute oidc token request
   * @param authorizationCode
   * @return
   */
  private def sendOIDCTokenRequest(
    authorizationCode: AuthorizationCode
  ): Fu[HTTPResponse] = buildOIDCTokenRequest(authorizationCode).flatMap { request =>
    try {
      fuccess(request.toHTTPRequest.send())
    } catch {
      case se: SerializeException =>
        fufail(s"Failed to send oidc code verification request (SerializeException) $se")

      case e: IOException =>
        fufail(s"Failed to send oidc code verification request (IOException) $e")
    }
  }

  /**
   * Create oidc token request
   * @param authorizationCode
   * @return
   */
  private def buildOIDCTokenRequest(
    authorizationCode: AuthorizationCode
  ): Fu[TokenRequest] = metadata.flatMap { metadata =>
    fuccess(
      new TokenRequest(
        metadata.getTokenEndpointURI(),
        new ClientSecretBasic(oidc.clientID, oidc.clientSecret),
        new AuthorizationCodeGrant(authorizationCode, oidc.signinCallbackUrl),
        OIDC_SCOPE
      )
    )
  }

  /**
   * Validate the token response.
   *
   * @param response Token response
   * @param metadata OpenID Provider metadata
   * @param nonce    Nonce
   * @return Claims
   */
  private def validateOIDCTokenResponse(
    response: OIDCTokenResponse,
    nonce: Nonce
  ): Fu[IDTokenClaimsSet] = Option(response.getOIDCTokens.getIDToken) match {
    case Some(jwt) =>
      metadata.flatMap { metadata =>
        val validator = oidc.jwsAlgorithm map { jwsAlgorithm =>
          new IDTokenValidator(metadata.getIssuer, oidc.clientID, jwsAlgorithm, metadata.getJWKSetURI.toURL,
            new DefaultResourceRetriever(JWK_REQUEST_TIMEOUT, JWK_REQUEST_TIMEOUT))
        } getOrElse {
          new IDTokenValidator(metadata.getIssuer, oidc.clientID)
        }

        try {
          fuccess(validator.validate(jwt, nonce))
        } catch {
          case e @ (_: BadJOSEException | _: JOSEException) =>
            fufail(s"OIDC ID token has error: $e")
        }
      }
    case None =>
      fufail(s"OIDC token response does not have a valid ID token: ${response.toJSONObject}")
  }

  /**
   * Retrieve user info from oidc provider
   * @param token
   * @param oidc
   * @return
   */
  def obtainUserInfo(
    token: BearerAccessToken
  ): Fu[UserInfo] = sendUserInfoRequest(token).flatMap { httpResponse =>
    try {
      UserInfoResponse.parse(httpResponse) match {
        case successResponse: UserInfoSuccessResponse =>
          fuccess(successResponse.getUserInfo())
        case errorResponse: UserInfoErrorResponse =>
          fufail(s"OIDC token response has error: ${errorResponse.getErrorObject.toJSONObject}")
      }
    } catch {
      case e: ParseException =>
        fufail(s"UserInfo response has error: $e")
    }
  }

  private def sendUserInfoRequest(
    token: BearerAccessToken
  ): Fu[HTTPResponse] = buildUserInfoRequest(token).flatMap { request =>
    try {
      fuccess(request.toHTTPRequest.send())
    } catch {
      case se: SerializeException =>
        fufail(s"Failed to send oidc user info request (SerializeException) $se")
      case e: IOException =>
        fufail(s"Failed to send oidc user info request (IOException) $e")
    }
  }

  private def buildUserInfoRequest(
    token: BearerAccessToken
  ): Fu[UserInfoRequest] = metadata.flatMap { metadata =>
    fuccess(
      new UserInfoRequest(metadata.getUserInfoEndpointURI(), token)
    )
  }

  /**
   * Retrieve oidc provider metadata
   *
   * @param issuer
   * @return Either[String, OIDCProviderMetadata]
   */
  private def getMetadata(issuer: Issuer): Fu[OIDCProviderMetadata] =
    try {
      fuccess(OIDCProviderMetadata.resolve(issuer))
    } catch {
      case e @ (_: GeneralException | _: IOException) =>
        fufail(s"Unable retrieve OIDC metadata")
    }
}