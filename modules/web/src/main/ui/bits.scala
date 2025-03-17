package lila.web
package ui
import lila.ui.*

import ScalatagsTemplate.{ *, given }

object bits:

  lazy val stage = a(
    href  := "https://live.chess-online.com",
    style := """
background: #7f1010;
color: #fff;
position: fixed;
bottom: 0;
left: 0;
padding: .5em 1em;
border-top-right-radius: 3px;
z-index: 99;
"""
  ):
    "This is an empty Chess-Online Arena preview website, go to live.chess-online.com instead"

  val connectLinks: Frag =
    div(cls := "connect-links")(
      a(
        href := routes.Main.externalLink("vk"),
        targetBlank,
        noFollow
      )("VK"),
      a(
        href := routes.Main.externalLink("ok"),
        targetBlank,
        noFollow
      )("OK"),
      a(
        href := routes.Main.externalLink("facebook"),
        targetBlank,
        noFollow
      )("Facebook"),
      a(
        href := routes.Main.externalLink("telegram"),
        targetBlank,
        noFollow
      )("Telegram"),
      a(
        href := routes.Main.externalLink("discord"),
        targetBlank,
        noFollow
      )("Discord")
    )

  def api = raw:
    """<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; style-src 'unsafe-inline'; script-src https://cdn.jsdelivr.net blob:; child-src blob:; connect-src https://raw.githubusercontent.com; img-src data: https://live.chess-online.com https://cdn.chess-online.com;">
    <title>Chess-Online.Com API reference</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>body { margin: 0; padding: 0; }</style>
  </head>
  <body>
    <redoc spec-url="https://raw.githubusercontent.com/lichess-org/api/master/doc/specs/lichess-api.yaml"></redoc>
    <script src="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"></script>
  </body>
</html>"""
