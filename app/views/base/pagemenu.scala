package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

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

/*

      <div class="header ">
      	<!-- START MOBILE SIDEBAR TOGGLE -->
      	<a href="#" class="btn-link toggle-sidebar d-lg-none pg-icon btn-icon-link" data-toggle="sidebar">
      		menu</a>

      	<!-- END MOBILE SIDEBAR TOGGLE -->
      	<div class="">
      		<div
      			class="brand inline   ">
      			<img src="assets/img/logo.png" alt="logo" data-src="assets/img/logo.png"
      				data-src-retina="assets/img/logo_2x.png" width="78" height="22">
      		</div>

      		<a href="#" class="search-link d-lg-inline-block d-none" data-toggle="search"><i
      				class="pg-icon">search</i>Type anywhere to <span class="bold">search</span></a>
      	</div>
      	<div class="d-flex align-items-center">
      		<!-- START User Info-->

      		<div class="dropdown pull-right d-lg-block d-none">
      			<button class="profile-dropdown-toggle" type="button" data-toggle="dropdown" aria-haspopup="true"
      				aria-expanded="false" aria-label="profile dropdown">
      				<span class="thumbnail-wrapper d32 circular inline">
      					<img src="assets/img/profiles/avatar.jpg" alt="" data-src="assets/img/profiles/avatar.jpg"
      						data-src-retina="assets/img/profiles/avatar_small2x.jpg" width="32" height="32">
      				</span>
      			</button>
      			<div class="dropdown-menu dropdown-menu-right profile-dropdown" role="menu">
      				<a href="#" class="dropdown-item"><span>Signed in as <br /><b>David Aunsberg</b></span></a>
      				<div class="dropdown-divider"></div>
      				<a href="#" class="dropdown-item">Your Profile</a>
      				<a href="#" class="dropdown-item">Your Activity</a>
      				<a href="#" class="dropdown-item">Your Archive</a>
      				<div class="dropdown-divider"></div>
      				<a href="#" class="dropdown-item">Features</a>
      				<a href="#" class="dropdown-item">Help</a>
      				<a href="#" class="dropdown-item">Settings</a>
      				<a href="#" class="dropdown-item">Logout</a>
      				<div class="dropdown-divider"></div>
      				<span class="dropdown-item fs-12 hint-text">Last edited by David<br />on Friday at 5:27PM</span>
      			</div>
      		</div>
      		<!-- END User Info-->
      	</div>
      </div>
 */
