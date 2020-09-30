package views.html.base

import lila.app.ui.ScalatagsTemplate._

object pagemenu {
  private val dataToggle = attr("data-toggle")

  st.div(cls := "header")(
    a(href := "#", cls := "btn-link toggle-sidebar d-lg-none pg-icon btn-icon-link", dataToggle := "sidebar")("menu"),
    st.div()(
      st.div(cls := "brand inline")(
        img(
          src := "https://cdn.chess-online.com/images/logos/nav-logo-uni.png",
          alt := "Chess-Online",
          style := "width:78px;height:22px;"
        )()
      ),
      a(cls := "search-link d-lg-inline-block d-none")()
    ),
    st.div()(

    )
  )
}
