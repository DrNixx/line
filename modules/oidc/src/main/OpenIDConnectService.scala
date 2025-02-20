package lila.oidc

import java.io.IOException

import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jose.JOSEException
import com.nimbusds.oauth2.sdk._
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.{ Issuer, State }
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator
import com.nimbusds.openid.connect.sdk.{ AuthenticationErrorResponse, _ }
import com.nimbusds.openid.connect.sdk.claims._

import scala.jdk.CollectionConverters._

// http://nemcio.cf/gitbucket/

final class OpenIDConnectService(
    val oidc: OIDC
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val metadata = getMetadata(oidc.issuer)

  private val JWK_REQUEST_TIMEOUT = 5000

  private val OIDC_SCOPE = new Scope(
    OIDCScopeValue.OPENID,
    OIDCScopeValue.EMAIL,
    OIDCScopeValue.PROFILE,
    OIDCScopeValue.ADDRESS
  )

  OIDC_SCOPE.add("profile_ex")

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
    params: Map[String, Seq[String]],
    state: State,
    nonce: Nonce
  ): Fu[UserInfo] =
    validateOIDCAuthenticationResponse(params, state) flatMap { authenticationResponse =>
      obtainOIDCToken(authenticationResponse.getAuthorizationCode, nonce) flatMap { token =>
        obtainUserInfo(token) flatMap { userInfo =>
          fuccess(userInfo)
        }
      }
    }

  private def converParams(params: Map[String, Seq[String]]) = {
    params.map { case (k, v) => (k, v.toList.asJava) }  asJava
  }

  /**
   * Validate the authentication response.
   *
   * @param params      Query parameters of the authentication response
   * @param state       State saved in the session
   * @return Authentication response
   */
  def validateOIDCAuthenticationResponse(params: Map[String, Seq[String]], state: State): Fu[AuthenticationSuccessResponse] =
    try {
      AuthenticationResponseParser.parse(oidc.signinCallbackUrl, converParams(params)) match {
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
  ): Fu[BearerAccessToken] = sendOIDCTokenRequest(authorizationCode).flatMap { httpResponse =>
    try {
      OIDCTokenResponseParser.parse(httpResponse) match {
        case response: OIDCTokenResponse =>
          validateOIDCTokenResponse(response, nonce) flatMap { claims =>
            Seq("sub").map(k => Option(claims.getStringClaim(k))) match {
              case Seq(Some(sub)) =>
                val successResponse = response.toSuccessResponse
                fuccess(successResponse.getOIDCTokens.getBearerAccessToken)
              case _ =>
                fufail(s"OIDC ID token must have an email claim: claims=${claims.toJSONObject}")
            }
          }
        case response: TokenErrorResponse =>
          fufail(s"OIDC token response has error: ${response.getErrorObject.toJSONObject}")
      }
    } catch {
      case e: ParseException =>
        fufail(s"OIDC token response has error: $e")
    }
  }

  /**
   * Execute oidc token request
   * @param authorizationCode OpentId authorization code
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
   * @param authorizationCode OpentId authorization code
   * @return
   */
  private def buildOIDCTokenRequest(
    authorizationCode: AuthorizationCode
  ): Fu[TokenRequest] = metadata.flatMap { metadata =>
    fuccess(
      new TokenRequest(
        metadata.getTokenEndpointURI,
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
   * @return
   */
  def obtainUserInfo(
    token: BearerAccessToken
  ): Fu[UserInfo] = sendUserInfoRequest(token).flatMap { httpResponse =>
    try {
      UserInfoResponse.parse(httpResponse) match {
        case successResponse: UserInfoSuccessResponse =>
          fuccess(successResponse.getUserInfo)
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
      new UserInfoRequest(metadata.getUserInfoEndpointURI, token)
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
