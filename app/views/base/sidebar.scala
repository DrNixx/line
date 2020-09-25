package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object sidebar {

  private val dataPageToggle            = attr("data-pages-toggle")
  private val dataTogglePin             = attr("data-toggle-pin")
  private val dataPages                 = attr("data-pages")
  private val dataIcon                  = attr("data-icon")
  private val dataIconAlt                  = attr("data-icon-alt")

  private def icon(name: String)(implicit ctx: Context) =
    (name != "") option span(cls := "icon-thumbnail")(i(dataIcon := name)())

  private def arrow()(implicit ctx: Context) =
    span(cls := "arrow")()

  private def menuItem(url: String, title: Frag, iname: String = "")(implicit ctx: Context) =
    li()(
      a(href := url)(title),
      icon(iname)
    )

  private def canSeeClasMenu(implicit ctx: Context) =
    ctx.hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))



  def apply()(implicit ctx: Context) =
    st.nav(cls := "page-sidebar", dataPages := "sidebar")(
      st.div(cls := "sidebar-header")(
        a(href := "/")(
          img(
            cls := "brand",
            src := "https://cdn.chess-online.com/images/logos/nav-logo-arena.png",
            alt := "Chess-Online"
          )
        ),
        st.div(cls := "sidebar-header-controls")(
          button(cls := "btn-link sidebar-slide-toggle", dataPageToggle := "#appMenu", `type` := "button")(
            i(dataIcon := "А")
          ),
          button(cls := "btn-link", dataTogglePin := "sidebar", `type` := "button")(
            i(dataIcon := "Д", dataIconAlt := "Е")
          ),
        )
      ),
      st.div(cls := "sidebar-menu")(
        ul(cls := "menu-items")(
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(trans.play()),
              arrow()
            ),
            icon("а"),
            ul(cls := "sub-menu")(
              if (ctx.noBot) menuItem("/?any#hook", trans.createAGame(), "О")
              else menuItem("/?any#friend", trans.playWithAFriend(), "О"),
              ctx.noBot option frag(
                menuItem(routes.Tournament.home().toString(), trans.arena.arenaTournaments(), "Т"),
                menuItem(routes.Swiss.home().toString(), trans.swiss.swissTournaments(), "?"),
                menuItem(routes.Simul.home().toString(), trans.simultaneousExhibitions(), ".")
              )
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(trans.learnMenu()),
              arrow()
            ),
            icon(":"),
            ul(cls := "sub-menu")(
              ctx.noBot option frag(
                menuItem(routes.Learn.index().toString(), trans.chessBasics(), "м"),
                menuItem(routes.Puzzle.home().toString(), trans.puzzles(), "к"),
                menuItem(routes.Practice.index().toString(), trans.practice(), "Ф"),
                menuItem(routes.Coordinate.home().toString(), trans.coordinates.coordinates(), "н")
              ),
              menuItem(routes.Study.allDefault(1).toString(), trans.studyMenu(), "4"),
              ctx.noKid option menuItem(routes.Coach.all(1).toString(), trans.coaches(), "е"),
              canSeeClasMenu option menuItem(routes.Clas.index().toString(), trans.clas.lichessClasses(), "и")
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(trans.watch()),
              arrow()
            ),
            icon("v"),
            ul(cls := "sub-menu")(
              menuItem(routes.Tv.index().toString(), "Chess TV", "1"),
              menuItem(routes.Tv.games().toString(), trans.currentGames(), "Q"),
              ctx.noKid option menuItem(routes.Streamer.index().toString(), trans.streamersMenu(), "п"),
              menuItem(routes.Relay.index().toString(), trans.broadcast.broadcasts(), "о"),
              ctx.noBot option menuItem(routes.Video.index().toString(), trans.videoLibrary(), "л")
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(trans.community()),
              arrow()
            ),
            icon("в"),
            ul(cls := "sub-menu")(
              menuItem(routes.User.list().toString(), trans.players(), "Й"),
              menuItem(routes.Team.home().toString(), trans.team.teams(), "К"),
              ctx.noKid option menuItem("https://www.chess-online.com/forums", trans.forum(), "Л")
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(trans.tools()),
              arrow()
            ),
            icon("г"),
            ul(cls := "sub-menu")(
              menuItem(routes.UserAnalysis.index().toString(), trans.analysis(), "A"),
              menuItem(s"${routes.UserAnalysis.index()}#explorer", trans.openingExplorer(), "]"),
              menuItem(routes.Editor.index().toString(), trans.boardEditor(), "6"),
              menuItem(routes.Importer.importGame().toString(), trans.importGame(), "/"),
              menuItem(routes.Search.index().toString(), trans.search.advancedSearch(), "y")
            )
          ),
        )
      )
    )
}

/*




 */
