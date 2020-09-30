package views
package html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object faq {

  def apply()(implicit ctx: Context) =
    help.layout(
      title = "Frequently Asked Questions",
      active = "faq",
      moreCss = cssTag("faq")
    ) {
      div(cls := "faq small-page box box-pad")()
    }
}
