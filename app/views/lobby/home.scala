package views.lobby

import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.Preload.Homepage
import lila.core.perf.UserWithPerfs

object home:

  def apply(homepage: Homepage)(using ctx: Context) =
    import homepage.*
    Page("")
      .copy(fullTitle = s"$siteName • ${trans.site.freeOnlineChess.txt()}".some)
      .js(
        PageModule(
          "lobby",
          Json
            .obj(
              "data"                    -> data,
              "showRatings"             -> ctx.pref.showRatings,
              "hasUnreadLichessMessage" -> hasUnreadLichessMessage
            )
            .add(
              "playban",
              playban.map: pb =>
                Json.obj("minutes" -> pb.mins, "remainingSeconds" -> (pb.remainingSeconds + 3))
            )
        )
      )
      .css("lobby")
      .csp(_.withAd)
      .graph(
        OpenGraph(
          image = staticAssetUrl("logo/chess-tile-wide.png").some,
          title = "The best free Chess server",
          url = netBaseUrl.value,
          description = trans.site.siteDescription.txt()
        )
      )
      .hrefLangs(lila.ui.LangPath("/")):
        given Option[UserWithPerfs] = homepage.me
        main(
          cls := List(
            "lobby"      -> true,
            "lobby-nope" -> (playban.isDefined || currentGame.isDefined || homepage.hasUnreadLichessMessage)
          )
        )(
          div(cls := "lobby__table")(
            div(cls := "lobby__start")(
              button(cls := "button button-metal", tpe := "button", trans.site.createAGame()),
              button(cls := "button button-metal", tpe := "button", trans.site.playWithAFriend()),
              button(cls := "button button-metal", tpe := "button", trans.site.playWithTheMachine())
            )
          ),
          currentGame
            .map(bits.currentGameInfo)
            .orElse:
              hasUnreadLichessMessage.option(bits.showUnreadLichessMessage)
            .orElse:
              playban.map(bits.playbanInfo)
            .getOrElse:
              if ctx.blind then blindLobby(blindGames) else bits.lobbyApp
          ,
          div(cls := "lobby__side")(
            ctx.blind.option(h2("Highlights")),
            ctx.kid.no.option(
              st.section(cls := "lobby__streams")(
                views.streamer.bits.liveStreams(streams),
                streams.live.streams.nonEmpty.option(
                  a(href := routes.Streamer.index(), cls := "more")(
                    trans.site.streamersMenu(),
                    " »"
                  )
                )
              )
            ),
            div(cls := "lobby__spotlights"):
              val eventTags = events.map(bits.spotlight)
              val relayTags = views.relay.ui.spotlight(relays)
              frag(
                eventTags,
                relayTags,
                ctx.noBot.option {
                  val nbManual = eventTags.size + relayTags.size
                  val simulBBB = simuls.find(isFeaturable(_) && nbManual < 4)
                  val nbForced = nbManual + simulBBB.size.toInt
                  val tourBBBs = if nbForced > 3 then 0 else if nbForced == 3 then 1 else 3 - nbForced
                  frag(
                    lila.tournament.Spotlight.select(tours, tourBBBs).map {
                      views.tournament.list.homepageSpotlight(_)
                    },
                    swiss.ifTrue(nbForced < 3).map(views.swiss.ui.homepageSpotlight),
                    simulBBB.map(views.simul.ui.homepageSpotlight)
                  )
                }
              )
            ,
            ctx.isAuth.option(
              div(cls := "lobby__timeline")(
                ctx.blind.option(h2("Timeline")),
                views.timeline.entries(userTimeline),
                userTimeline.nonEmpty.option(
                  a(cls := "more", href := routes.Timeline.home)(
                    trans.site.more(),
                    " »"
                  )
                )
              )
            )
          ),
          featured.map: g =>
            div(cls := "lobby__tv"):
              views.game.mini(Pov.naturalOrientation(g), tv = true)
          ,
          puzzle.map: p =>
            views.puzzle.bits.dailyLink(p)(cls := "lobby__puzzle"),
          div(cls := "lobby__blog ublog-post-cards"):
            ublogPosts
              .filter(_.isLichess || ctx.kid.no)
              .take(9)
              .map:
                views.ublog.ui.card(_, showAuthor = views.ublog.ui.ShowAt.bottom, showIntro = false)
          ,
          ctx.noBot.option(bits.underboards(tours, simuls)),
          div(cls := "lobby__feed"):
            views.feed.lobbyUpdates(lastUpdates)
          ,
          div(cls := "lobby__support")(
          )
        )
