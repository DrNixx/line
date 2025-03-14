package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given

final class Msg(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me ?=>
    Redirect("https://www.chess-online.com/pm/inbox")
  }

  def convo(userId: UserId, before: Option[Long] = None) = Auth { _ ?=> me ?=>
    if userId.value == "new"
    then Redirect("https://www.chess-online.com/pm/compose")
    else Redirect(s"https://www.chess-online.com/pm/compose/$userId")
  }

  def search(q: String) = Auth { _ ?=> me ?=>
    Redirect("https://www.chess-online.com/pm/inbox")
  }

  def unreadCount = Auth { _ ?=> me ?=>
    JsonOk:
      env.msg.compat.unreadCount(me)
  }

  def convoDelete(username: UserStr) = Auth { _ ?=> me ?=>
    env.msg.api.delete(username) >>
      JsonOk(inboxJson)
  }

  def compatCreate = AuthBody { ctx ?=> me ?=>
    ctx.kid.no
      .so(ctx.noBot)
      .so(
        env.msg.compat.create
          .fold(
            doubleJsonFormError,
            _.map: id =>
              Ok(Json.obj("ok" -> true, "id" -> id))
          )
      )
  }

  def apiPost(username: UserStr) = AuthOrScopedBody(_.Msg.Write) { ctx ?=> me ?=>
    val userId = username.id
    Redirect("https://www.chess-online.com/pm/inbox")
  }

  private def inboxJson(using me: Me) =
    env.msg.api.myThreads.flatMap(env.msg.json.threads).map { threads =>
      import lila.common.Json.lightUserWrites
      Json.obj(
        "me"       -> Json.toJsObject(me.light).add("bot" -> me.isBot),
        "contacts" -> threads
      )
    }
