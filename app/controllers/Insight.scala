package controllers
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.*

import lila.app.{ *, given }
import lila.core.i18n.Translate
import lila.insight.{ InsightDimension, InsightMetric }

final class Insight(env: Env) extends LilaController(env):

  def refresh(userId: UserId) = OpenOrScoped(): ctx ?=>
    AccessibleApi(userId): user =>
      env.insight.api.indexAll(user).inject(Ok)

  def index(userId: UserId) = OpenOrScoped(): ctx ?=>
    Accessible(userId): user =>
      negotiate(
        html = doPath(user, InsightMetric.MeanCpl.key, InsightDimension.Perf.key, ""),
        json = env.insight.api.userStatus(user).map { status =>
          Ok(Json.obj("status" -> status.toString))
        }
      )

  def path(userId: UserId, metric: String, dimension: String, filters: String) = Open:
    Accessible(userId) { doPath(_, metric, dimension, ~lila.common.String.decodeUriPath(filters)) }

  private def doPath(user: lila.user.User, metric: String, dimension: String, filters: String)(using
      Context
  ) =
    import lila.insight.InsightApi.UserStatus.*
    env.insight.api.userStatus(user).flatMap {
      case NoGame => Ok.page(views.site.message.insightNoGames(user))
      case Empty  => Ok.page(views.insight.empty(user))
      case s =>
        for
          insightUser <- env.insight.api.insightUser(user)
          prefId      <- env.insight.share.getPrefId(user)
          page <- renderPage:
            views.insight.index(
              u = user,
              insightUser = insightUser,
              prefId = prefId,
              ui = env.insight.jsonView
                .ui(insightUser.families, insightUser.openings, asMod = isGrantedOpt(_.ViewBlurs)),
              question = env.insight.jsonView.question(metric, dimension, filters),
              stale = s == Stale
            )
        yield Ok(page)
    }

  def json(userId: UserId) =
    OpenOrScopedBody(parse.json)(): ctx ?=>
      AccessibleApi(userId) { processQuestion(_, ctx.body) }

  private def processQuestion(user: lila.user.User, body: Request[JsValue])(using Translate) =
    body.body
      .validate[lila.insight.JsonQuestion]
      .fold(
        err => BadRequest(jsonError(err.toString)).toFuccess,
        _.question.fold(BadRequest.toFuccess): q =>
          env.insight.api
            .ask(q, user)
            .flatMap(lila.insight.Chart.fromAnswer(env.user.lightUser))
            .map(env.insight.jsonView.chartWrites.writes)
            .map { Ok(_) }
      )

  private def Accessible(userId: UserId)(f: lila.user.User => Fu[Result])(using Context) =
    isAccessible(userId)(f, u => Forbidden.page(views.insight.forbidden(u)))

  private def AccessibleApi(userId: UserId)(f: lila.user.User => Fu[Result])(using Context) =
    isAccessible(userId)(f, _ => Forbidden)

  private def isAccessible(
      userId: UserId
  )(f: lila.user.User => Fu[Result], fallback: lila.user.User => Fu[Result])(using Context): Fu[Result] =
    Found(meOrFetch(userId)): u =>
      env.insight.share
        .grant(u)(using ctx.me)
        .flatMap:
          if _ then f(u)
          else fallback(u)
