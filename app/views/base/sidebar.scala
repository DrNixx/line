package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object sidebar {

  private def icon(name: String)(implicit ctx: Context) =
    (name != "") option span(cls := "icon-thumbnail")(i(cls := name)())

  private def arrow()(implicit ctx: Context) =
    span(cls := "arrow")()

  private def menuItem(url: String, title: Frag, iname: String = "")(implicit ctx: Context) =
    li()(
      a(href := url)(title),
      icon(iname)
    )

  private def canSeeClasMenu(implicit ctx: Context) =
    ctx.hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))

  private val dataPages                 = attr("data-pages")

  def apply()(implicit ctx: Context) =
    st.nav(cls := "page-sidebar", dataPages := "sidebar")(
      st.div(cls := "sidebar-header")(
        a(href := "/")(
          img(
            src := "https://cdn.chess-online.com/images/logos/nav-logo-black.png",
            alt := "Chess-Online",
            style := "width:127px;height:24px;"
          )
        )
      ),
      st.div(cls := "sidebar-menu")(
        ul(cls := "menu-items")(
          li(cls := "m-t-20")(
            a(href := "javascript:;")(
              span(cls := "title")(
                trans.play(),
                arrow()
              ),
              icon("play"),
              ul(cls := "sub-menu")(
                if (ctx.noBot) menuItem("/?any#hook", trans.createAGame())
                else menuItem("/?any#friend", trans.playWithAFriend()),
                ctx.noBot option frag(
                  menuItem(routes.Tournament.home().toString(), trans.arena.arenaTournaments()),
                  menuItem(routes.Swiss.home().toString(), trans.swiss.swissTournaments()),
                  menuItem(routes.Simul.home().toString(), trans.simultaneousExhibitions())
                )
              )
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(
                trans.learnMenu(),
                arrow()
              ),
              icon("learn"),
              ul(cls := "sub-menu")(
                ctx.noBot option frag(
                  menuItem(routes.Learn.index().toString(), trans.chessBasics()),
                  menuItem(routes.Puzzle.home().toString(), trans.puzzles()),
                  menuItem(routes.Practice.index().toString(), trans.practice()),
                  menuItem(routes.Coordinate.home().toString(), trans.coordinates.coordinates())
                ),
                menuItem(routes.Study.allDefault(1).toString(), trans.studyMenu()),
                ctx.noKid option menuItem(routes.Coach.all(1).toString(), trans.coaches()),
                canSeeClasMenu option menuItem(routes.Clas.index().toString(), trans.clas.lichessClasses())
              )
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(
                trans.watch(),
                arrow()
              ),
              icon("watch"),
              ul(cls := "sub-menu")(
                menuItem(routes.Tv.index().toString(), "Lichess TV"),
                menuItem(routes.Tv.games().toString(), trans.currentGames()),
                ctx.noKid option menuItem(routes.Streamer.index().toString(), trans.streamersMenu()),
                menuItem(routes.Relay.index().toString(), trans.broadcast.broadcasts()),
                ctx.noBot option menuItem(routes.Video.index().toString(), trans.videoLibrary())
              )
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(
                trans.community(),
                arrow()
              ),
              icon("community"),
              ul(cls := "sub-menu")(
                menuItem(routes.User.list().toString(), trans.players()),
                menuItem(routes.Team.home().toString(), trans.team.teams()),
                ctx.noKid option menuItem(routes.ForumCateg.index().toString(), trans.forum())
              )
            )
          ),
          li()(
            a(href := "javascript:;")(
              span(cls := "title")(
                trans.tools(),
                arrow()
              ),
              icon("tools"),
              ul(cls := "sub-menu")(
                menuItem(routes.UserAnalysis.index().toString(), trans.analysis()),
                menuItem(s"${routes.UserAnalysis.index()}#explorer", trans.openingExplorer()),
                menuItem(routes.Editor.index().toString(), trans.boardEditor()),
                menuItem(routes.Importer.importGame().toString(), trans.importGame()),
                menuItem(routes.Search.index().toString(), trans.search.advancedSearch())
              )
            )
          ),
        )
      )
    )
}

/*




 */
