package views.html
package userTournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.user.User

import controllers.routes

object bits {

  def best(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(implicit ctx: Context) =
    layout(
      u,
      title = s"${u.username} best tournaments",
      path = "best",
      moreJs = infiniteScrollTag
    ) {
      views.html.userTournament.list(u, "best", pager, "BEST")
    }

  def recent(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(implicit ctx: Context) =
    layout(
      u,
      title = s"${u.username} recent tournaments",
      path = "recent",
      moreJs = infiniteScrollTag
    ) {
      views.html.userTournament.list(u, "recent", pager, pager.nbResults.toString)
    }

  def layout(u: User, title: String, path: String, moreJs: Frag = emptyFrag)(
      body: Frag
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("user-tournament"),
      moreJs = moreJs
    ) {
      main(cls := "page-menu")(
        st.nav(cls := "page-menu__menu subnav")(
          a(cls := path.active("created"), href := routes.UserTournament.path(u.id, "created"))(
            "Created"
          ),
          a(cls := path.active("recent"), href := routes.UserTournament.path(u.id, "recent"))(
            "Recently played"
          ),
          a(cls := path.active("best"), href := routes.UserTournament.path(u.id, "best"))(
            "Best results"
          ),
          a(cls := path.active("chart"), href := routes.UserTournament.path(u.id, "chart"))(
            "Stats"
          )
        ),
        div(cls := "page-menu__content box")(body)
      )
    }
}
