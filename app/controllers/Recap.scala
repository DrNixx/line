package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.recap.Recap as RecapModel
import lila.recap.Recap.Availability

final class Recap(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me ?=>
    Redirect(routes.Recap.user(me.userId))
  }

  def user(userId: UserId) = RecapPage(userId) { _ ?=> user => data =>
    negotiate(
      html = Ok.page(views.recap.home(data, user)),
      json = data match
        case Availability.Available(data) => Ok(data).toFuccess
        case Availability.Queued(_)       => env.recap.api.awaiter(user).map(Ok(_))
    )
  }

  private def RecapPage(
      userId: UserId
  )(f: Context ?=> UserModel => Availability => Fu[Result]): EssentialAction =
    Auth { ctx ?=> me ?=>
      def proceed(user: lila.core.user.User) = for
        av  <- env.recap.api.availability(user)
        res <- f(using ctx.updatePref(_.forceDarkBg))(user)(av)
      yield res
      if me.is(userId) then proceed(me)
      else if isGranted(_.SeeInsight) || !env.net.isProd then Found(env.user.api.byId(userId))(proceed)
      else Redirect(routes.Recap.home).toFuccess
    }
