package views.html.account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object layout {

  def apply(
      title: String,
      active: String,
      evenMoreCss: Frag = emptyFrag,
      evenMoreJs: Frag = emptyFrag
  )(body: Frag)(implicit ctx: Context): Frag =
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("account"), evenMoreCss),
      moreJs = frag(jsModule("account"), evenMoreJs)
    ) {
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "account page-menu")(
        st.nav(cls := "page-menu__menu subnav")(
          lila.pref.PrefCateg.all.map { categ =>
            a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
              bits.categName(categ)
            )
          },
          a(activeCls("kid"), href := routes.Account.kid())(
            trans.kidMode()
          ),
          div(cls := "sep"),
          a(activeCls("editProfile"), href := routes.Account.profile())(
            trans.editProfile()
          ),
          isGranted(_.Coach) option a(activeCls("coach"), href := routes.Coach.edit())(
            trans.coach.lichessCoach()
          ),
          div(cls := "sep"),
          a(activeCls("password"), href := "https://passport.chess-online.com/manage/change-password")(
            trans.changePassword()
          ),
          a(activeCls("email"), href := "https://passport.chess-online.com/manage/change-email")(
            trans.changeEmail()
          ),
          a(activeCls("username"), href := "https://passport.chess-online.com/profile/edit-username")(
            trans.changeUsername()
          ),
          a(activeCls("security"), href := "https://passport.chess-online.com/profile")(
            trans.security()
          ),
          div(cls := "sep"),
          a(href := routes.Plan.index())(trans.patron.lichessPatron()),
          div(cls := "sep"),
          a(activeCls("oauth.token"), href := routes.OAuthToken.index())(
            "API Access tokens"
          ),
          ctx.noBot option a(activeCls("oauth.app"), href := routes.OAuthApp.index())("OAuth Apps"),
          div(cls := "sep"),
          a(activeCls("close"), href := "https://passport.chess-online.com/manage/delete-account")(
            trans.settings.closeAccount()
          )
        ),
        div(cls := "page-menu__content")(body)
      )
    }
}
