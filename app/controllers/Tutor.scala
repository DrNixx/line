package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.LilaOpeningFamily
import lila.core.perf.UserWithPerfs
import lila.rating.PerfType
import lila.tutor.{ TutorFullReport, TutorPerfReport, TutorQueue }

final class Tutor(env: Env) extends LilaController(env):

  def home = Secure(_.Beta) { _ ?=> me ?=>
    Redirect(routes.Tutor.user(me.userId))
  }

  def user(userId: UserId) = TutorPage(userId) { _ ?=> user => av =>
    Ok.page(views.tutor.home(av, user))
  }

  def perf(userId: UserId, perf: PerfKey) = TutorPerfPage(userId, perf) { _ ?=> user => full => perf =>
    Ok.page(views.tutor.perf(full.report, perf, user))
  }

  def openings(userId: UserId, perf: PerfKey) = TutorPerfPage(userId, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.tutor.openingUi.openings(perf, user))
  }

  def opening(userId: UserId, perf: PerfKey, color: Color, opName: String) =
    TutorPerfPage(userId, perf) { _ ?=> user => _ => perf =>
      LilaOpeningFamily
        .find(opName)
        .flatMap(perf.openings(color).find)
        .fold(Redirect(routes.Tutor.openings(user.id, perf.perf.key)).toFuccess): family =>
          env.puzzle.opening.find(family.family.key).flatMap { puzzle =>
            Ok.page(views.tutor.opening(perf, family, color, user, puzzle))
          }
    }

  def skills(userId: UserId, perf: PerfKey) = TutorPerfPage(userId, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.tutor.perf.skills(perf, user))
  }

  def phases(userId: UserId, perf: PerfKey) = TutorPerfPage(userId, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.tutor.perf.phases(perf, user))
  }

  def time(userId: UserId, perf: PerfKey) = TutorPerfPage(userId, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.tutor.perf.time(perf, user))
  }

  def refresh(userId: UserId) = TutorPageAvailability(userId) { _ ?=> user => availability =>
    env.tutor.api.request(user, availability).inject(redirHome(user))
  }

  private def TutorPageAvailability(
      userId: UserId
  )(f: Context ?=> UserModel => TutorFullReport.Availability => Fu[Result]): EssentialAction =
    Secure(_.Beta) { ctx ?=> me ?=>
      def proceed(user: UserWithPerfs) = env.tutor.api.availability(user).flatMap(f(user.user))
      if me.is(userId) then env.user.api.withPerfs(me.value).flatMap(proceed)
      else
        Found(env.user.api.withPerfs(userId)): user =>
          if isGranted(_.SeeInsight) then proceed(user)
          else
            (user.enabled.yes.so(env.clas.api.clas.isTeacherOf(me, user.id))).flatMap {
              if _ then proceed(user) else notFound
            }
    }

  private def TutorPage(
      userId: UserId
  )(f: Context ?=> UserModel => TutorFullReport.Available => Fu[Result]): EssentialAction =
    TutorPageAvailability(userId) { _ ?=> user => availability =>
      availability match
        case TutorFullReport.InsufficientGames =>
          BadRequest.page(views.tutor.home.empty.insufficientGames(user))
        case TutorFullReport.Empty(in: TutorQueue.InQueue) =>
          for
            waitGames <- env.tutor.queue.waitingGames(user)
            user      <- env.user.api.withPerfs(user)
            page      <- renderPage(views.tutor.home.empty.queued(in, user, waitGames))
          yield Accepted(page)
        case TutorFullReport.Empty(_)             => Accepted.page(views.tutor.home.empty.start(user))
        case available: TutorFullReport.Available => f(user)(available)
    }

  private def TutorPerfPage(userId: UserId, perf: PerfKey)(
      f: Context ?=> UserModel => TutorFullReport.Available => TutorPerfReport => Fu[Result]
  ): EssentialAction =
    TutorPage(userId) { _ ?=> user => availability =>
      availability match
        case full @ TutorFullReport.Available(report, _) =>
          report(perf).fold(redirHome(user).toFuccess):
            f(user)(full)
    }

  private def redirHome(user: UserModel) = Redirect(routes.Tutor.user(user.id))
