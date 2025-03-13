package lila.web
package ui

import scalalib.model.Days
import lila.common.HTTPRequest
import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class SideNav(helpers: Helpers):
  import helpers.{ *, given }

  private val dataPageToggle = attr("data-pages-toggle")
  private val dataTogglePin  = attr("data-toggle-pin")
  private val dataPages      = attr("data-pages")
  private val dataIcon       = attr("data-icon")
  private val dataIconAlt    = attr("data-icon-alt")

  private def icon(name: String) =
    Option.when(!name.isEmpty):
      span(cls := "icon-thumbnail")(i(cls := "co", dataIcon := name)())

  private def arrow() =
    span(cls := "arrow")()

  private def menuItem(url: String, title: Frag, iname: String = "", details: Frag = emptyFrag) =
    li()(
      a(href := url)(
        if details.equals(emptyFrag) then title
        else
          frag(
            span(cls := "title")(title),
            span(cls := "details")(details)
          )
      ),
      icon(iname)
    )

  def apply(hasClas: Boolean, hasDgt: Boolean)(using ctx: Context) =
    val canSeeClasMenu = hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))
    val noIframe       = !HTTPRequest.isIframe(ctx.req)
    st.nav(cls := "page-sidebar", dataPages := "sidebar")(
      div(cls := "sidebar-header")(
        a(href := "/")(
          img(
            cls := "brand",
            src := "https://cdn.chess-online.com/images/logos/nav-logo-arena.png",
            alt := "Chess-Online"
          )
        ),
        div(cls := "sidebar-header-controls")(
          button(cls := "btn-link sidebar-slide-toggle", dataPageToggle := "#appMenu", `type` := "button")(
            i(dataIcon := "А")
          ),
          button(cls := "btn-link", dataTogglePin := "sidebar", `type` := "button")(
            i(cls := "co", dataIcon := "Д", dataIconAlt := "Е")
          )
        )
      ),
      div(cls := "sidebar-menu")(
        ul(cls := "menu-items")(
          Option.when(noIframe):
            menuItem("https://www.chess-online.com/", trans.site.homeMenu(), "Э")
          ,
          li()(
            a(href := "#")(
              span(cls := "title")(trans.site.play()),
              arrow()
            ),
            icon("а"),
            ul(cls := "sub-menu")(
              if ctx.userId.isEmpty then
                menuItem(
                  s"${langHref("/")}?any#hook",
                  trans.site.playAsGuest(),
                  "О",
                  trans.site.playAsGuestDesc()
                )
              else if ctx.noBot then
                menuItem("https://www.chess-online.com/chess/create", trans.site.createAGame(), "О")
              else menuItem("/?any#friend", trans.site.playWithAFriend(), "О"),
              ctx.noBot.option(
                frag(
                  menuItem(langHref(routes.Tournament.home), trans.arena.arenaTournaments(), "Т"),
                  menuItem(langHref(routes.Swiss.home), trans.swiss.swissTournaments(), "?"),
                  menuItem(langHref(routes.Simul.home), trans.site.simultaneousExhibitions(), "."),
                  hasDgt.option(menuItem(routes.DgtCtrl.index.url, trans.dgt.dgtBoard(), "T"))
                )
              )
            )
          ),
          Option.when(ctx.noBot):
            frag(
              li()(
                a(href := "#")(
                  span(cls := "title")(trans.site.tournaments()),
                  arrow()
                ),
                icon("р"),
                ul(cls := "sub-menu")(
                  menuItem(langHref(routes.Tournament.home), trans.arena.arenaTournaments(), "5"),
                  menuItem(langHref(routes.Swiss.home), trans.swiss.swissTournaments(), "с"),
                  menuItem("https://www.chess-online.com/tournaments", trans.site.corrsTournaments(), "Ь")
                )
              )
            )
          ,
          li()(
            a(href := "#")(
              span(cls := "title")(trans.site.learnMenu()),
              arrow()
            ),
            icon(":"),
            ul(cls := "sub-menu")(
              Option.when(ctx.noBot):
                frag(
                  menuItem(langHref(routes.Learn.index), trans.site.chessBasics(), "м"),
                  menuItem(routes.Practice.index.url, trans.site.practice(), "Ф"),
                  menuItem(langHref(routes.Coordinate.home), trans.coordinates.coordinates(), "н")
                )
              ,
              menuItem(langHref(routes.Study.allDefault()), trans.site.studyMenu(), "4"),
              ctx.kid.no.option(menuItem(routes.Coach.all(1).toString(), trans.site.coaches(), "е")),
              canSeeClasMenu.option(menuItem(routes.Clas.index.url, trans.clas.lichessClasses(), "и"))
            )
          ),
          Option.when(ctx.noBot):
            li()(
              a(href := "#")(
                span(cls := "title")(trans.site.puzzles()),
                arrow()
              ),
              icon("к"),
              ul(cls := "sub-menu")(
                menuItem(langHref(routes.Puzzle.home.url), trans.site.puzzles(), "к"),
                menuItem(langHref(routes.Puzzle.themes), trans.puzzle.puzzleThemes(), "м"),
                menuItem(
                  routes.Puzzle.dashboard(Days(30), "home", none).url,
                  trans.puzzle.puzzleDashboard(),
                  "Ф"
                ),
                menuItem(langHref(routes.Puzzle.streak), trans.puzzle.puzzleStreak(), "."),
                menuItem(langHref(routes.Storm.home), trans.puzzle.puzzleStorm(), "Ю"),
                menuItem(langHref(routes.Racer.home), trans.puzzle.puzzleRacer(), "Я")
              )
            )
          ,
          li()(
            a(href := "#")(
              span(cls := "title")(trans.site.community()),
              arrow()
            ),
            icon("в"),
            ul(cls := "sub-menu")(
              menuItem(routes.User.list.url, trans.site.players(), "т"),
              menuItem("https://www.chess-online.com/teams/list", trans.team.teams(), "у"),
              menuItem("https://www.chess-online.com/groups/social", trans.team.clubs(), "ф"),
              menuItem("https://www.chess-online.com/groups/play", trans.team.playGroups(), "х")
            )
          ),
          li()(
            a(href := "#")(
              span(cls := "title")(trans.site.watch()),
              arrow()
            ),
            icon("v"),
            ul(cls := "sub-menu")(
              menuItem(routes.Tv.index.url, "Chess-Online TV", "1"),
              menuItem(routes.Tv.games.url, trans.site.currentGames(), "Q"),
              (ctx.kid.no && ctx.noBot)
                .option(menuItem(routes.Streamer.index(1).url, trans.site.streamersMenu(), "п")),
              menuItem(routes.RelayTour.index(1).url, trans.broadcast.broadcasts(), "о")
            )
          ),
          Option.when(ctx.noBot):
            frag(
              li()(
                a(href := "#")(
                  span(cls := "title")(trans.site.library()),
                  arrow()
                ),
                icon("ш"),
                ul(cls := "sub-menu")(
                  menuItem("https://www.chess-online.com/library", trans.site.chessBooks(), "щ"),
                  menuItem("https://www.chess-online.com/encyclopedia", trans.site.worldTopPlayers(), "З"),
                  ctx.noBot.option(menuItem(routes.Video.index.url, trans.site.videoLibrary(), "л"))
                )
              )
            )
          ,
          li()(
            a(href := "#")(
              span(cls := "title")(trans.site.tools()),
              arrow()
            ),
            icon("г"),
            ul(cls := "sub-menu")(
              menuItem(routes.UserAnalysis.index.url, trans.site.analysis(), "A"),
              menuItem(routes.Opening.index().url, trans.site.openings(), "]"),
              menuItem(routes.Editor.index.url, trans.site.boardEditor(), "6"),
              menuItem(routes.Importer.importGame.url, trans.site.importGame(), "/"),
              menuItem(routes.Search.index(1).url, trans.search.advancedSearch(), "y")
            )
          ),
          li()(
            a(href := "#")(
              span(cls := "title")(trans.site.helpMenu()),
              arrow()
            ),
            icon("ъ"),
            ul(cls := "sub-menu")(
              menuItem("https://www.chess-online.com/help/index", trans.site.information()),
              menuItem("https://www.chess-online.com/feedback", trans.site.feedback())
            )
          ),
          (noIframe && ctx.kid.no)
            .option(menuItem("https://www.chess-online.com/forums", trans.site.forum(), "Л")),
          (noIframe && ctx.kid.no)
            .option(menuItem("https://www.chess-online.com/membership/club", trans.site.membership(), "ы"))
        )
      )
    )
