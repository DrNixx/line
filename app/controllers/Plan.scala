package controllers

import lila.app._

final class Plan(env: Env)(implicit system: akka.actor.ActorSystem) extends LilaController(env) {

  private val logger = lila.log("plan")

  def index =
    Open { implicit ctx =>
      Redirect("https://www.chess-online.com/membership/club").fuccess
    }

  def list =
    Open { implicit ctx =>
      Redirect("https://www.chess-online.com/membership/club").fuccess
    }

  def features =
    Open { implicit ctx =>
      Redirect("https://www.chess-online.com/membership/club").fuccess
    }

  def switch =
    Open { implicit ctx =>
      Redirect("https://www.chess-online.com/membership/club").fuccess
    }


  def cancel =
    Open { implicit ctx =>
      Redirect("https://www.chess-online.com/membership/club").fuccess
    }

  def thanks =
    Open { implicit ctx =>
      Redirect("https://www.chess-online.com/membership/thanks").fuccess
    }
}
