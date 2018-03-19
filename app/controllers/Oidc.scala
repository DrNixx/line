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

  def callback = Open { implicit ctx =>
    implicit val req = ctx.req

    val redirectBackURI = req.flash("oidcBackUrl")
    val state = req.flash("oidcState")
    val nonce = req.flash("oidcNonce")

    api.authenticate(
      req.queryString.map { case (k, Seq(v)) => (k, v) },
      redirectBackURI,
      state,
      nonce
    )

    negotiate(
      html = Redirect(routes.Main.mobile).fuccess,
      api = _ => Ok(Json.obj("ok" -> true)).fuccess
    ) map (_ withCookies LilaCookie.newSession)
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
