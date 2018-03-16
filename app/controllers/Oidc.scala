package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.app._
import lila.common.{ LilaCookie, HTTPRequest, IpAddress, EmailAddress }

object Oidc extends LilaController {

  private val env = Env.oidc
  private def api = env.api

  def login = Open { implicit ctx =>
    implicit val req = ctx.req

    val authenticationRequest = api.getAuthenticationRequest()
    val redirectBackURI = req.session.data.getOrElse("redirect", "/")

    fuccess(
      Redirect(authenticationRequest.toURI.toString).flashing(
        "oidcBackUrl" -> redirectBackURI,
        "oidcState" -> authenticationRequest.getState().getValue(),
        "oidcNonce" -> authenticationRequest.getNonce().getValue()
      )
    )
  }

  def logout = Open { implicit ctx =>
    implicit val req = ctx.req
    req.session get "sessionId" foreach lila.security.Store.delete
    negotiate(
      html = Redirect(routes.Main.mobile).fuccess,
      api = _ => Ok(Json.obj("ok" -> true)).fuccess
    ) map (_ withCookies LilaCookie.newSession)
  }
}
