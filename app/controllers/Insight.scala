package controllers

import lila.api.Context
import lila.app._
import lila.insight.{ Metric, Dimension }
import play.api.mvc._
import views._

object Insight extends LilaController {

  private def env = Env.insight

  def refresh(userId: String) = Open { implicit ctx =>
    Accessible(userId) { user =>
      env.api indexAll user inject Ok
    }
  }

  def index(userId: String) = path(
    userId,
    metric = Metric.MeanCpl.key,
    dimension = Dimension.Perf.key,
    filters = ""
  )

  def path(userId: String, metric: String, dimension: String, filters: String) = Open { implicit ctx =>
    Accessible(userId) { user =>
      import lila.insight.InsightApi.UserStatus._
      env.api userStatus user flatMap {
        case NoGame => Ok(html.insight.noGame(user)).fuccess
        case Empty => Ok(html.insight.empty(user)).fuccess
        case s => for {
          cache <- env.api userCache user
          prefId <- env.share getPrefId user
        } yield Ok(html.insight.index(
          u = user,
          cache = cache,
          prefId = prefId,
          ui = env.jsonView.ui(cache.ecos),
          question = env.jsonView.question(metric, dimension, filters),
          stale = s == Stale
        ))
      }
    }
  }

  def json(userId: String) = OpenBody(BodyParsers.parse.json) { implicit ctx =>
    import lila.insight.JsonQuestion, JsonQuestion._
    Accessible(userId) { user =>
      ctx.body.body.validate[JsonQuestion].fold(
        err => BadRequest(jsonError(err.toString)).fuccess,
        qJson => qJson.question.fold(BadRequest.fuccess) { q =>
          env.api.ask(q, user) map
            lila.insight.Chart.fromAnswer(Env.user.lightUserSync) map
            env.jsonView.chart.apply map { Ok(_) }
        }
      )
    }
  }

  private def Accessible(userId: String)(f: lila.user.User => Fu[Result])(implicit ctx: Context) =
    lila.user.UserRepo byId userId flatMap {
      _.fold(notFound) { u =>
        env.share.grant(u, ctx.me) flatMap {
          _.fold(f(u), fuccess(Forbidden(html.insight.forbidden(u))))
        }
      }
    }
}
