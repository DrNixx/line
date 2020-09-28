package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object sitefooter {
  def apply()(implicit ctx: Context) =
    footer()(
      st.div(cls := "foot-left")(
        st.div()(
          span(cls := "copyright")(trans.copyright())
        ),
        st.div(cls := "rules")(
          a(href := "https://passport.chess-online.com/rules/legal")(trans.termsOfService()),
          a(href := "https://passport.chess-online.com/rules/privacy")(trans.privacy()),
        )
      ),
      st.div(cls := "foot-right")(
        st.div(cls := "basedon")(trans.basedOn()),
        st.div(cls := "withlove")(trans.withLove())
      )
    )
}
