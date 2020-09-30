package views.html.tv

import controllers.routes

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._

object embed {

  private val dataStreamUrl = attr("data-stream-url")

  def apply(pov: lila.game.Pov)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "Chess-Online.Com chess TV",
      cssModule = "tv.embed"
    )(
      dataStreamUrl := routes.Tv.feed(),
      div(id := "featured-game", cls := "embedded", title := "Chess-Online TV")(
        views.html.game.mini.noCtx(pov, tv = true, blank = true)
      ),
      cashTag,
      jsModule("tvEmbed")
    )
}
