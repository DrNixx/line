package lila.web
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class PageFooter(helpers: Helpers):
  import helpers.{ *, given }

  def apply()(using ctx: Context) =
    footer()(
      div(cls := "foot-left")(
        div()(
          span(cls := "copyright")(trans.site.copyright())
        ),
        div(cls := "rules")(
          a(href := "https://passport.chess-online.com/rules/legal")(trans.site.termsOfService()),
          a(href := "https://passport.chess-online.com/rules/privacy")(trans.site.privacy())
        )
      ),
      div(cls := "foot-right")(
        div(cls := "basedon")(trans.site.basedOn()),
        div(cls := "withlove")(trans.site.withLove())
      )
    )
