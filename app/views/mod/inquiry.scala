package views.mod

import lila.app.UiEnv.{ *, given }
import lila.report.Report
import lila.shutup.Analyser

object inquiry:

  lazy val ui = lila.mod.ui.ModInquiryUi(helpers)

  private def renderAtomText(atom: Report.Atom, highlight: Boolean)(using Translate) =
    val (link, text) = atom.parseFlag.match
      case Some(flag) => publicLineSource(flag.source).some -> flag.quotes.mkString("\n")
      case None       => None                               -> atom.text
    frag(
      link,
      " ",
      if highlight then Analyser.highlightBad(text) else frag(text),
      " "
    )

  def apply(in: lila.mod.Inquiry)(using ctx: Context) =
    div(id := "inquiry", data("username") := in.user.user.username)(
      i(title := "Costello the Inquiry Octopus", cls := "costello"),
      div(cls := "meat")(
        userLink(in.user.user, withPerfRating = in.user.perfs.some, params = "?mod"),
        div(cls := "docs reports")(
          div(cls := "expendable")(in.allReports.map(ui.renderReport(renderAtomText)))
        ),
        ui.modLog(in.history),
        ui.noteZone(in.user.user, in.notes)
      ),
      div(cls := "links")(
        isGranted(_.MarkBooster).option {
          val searchUrl = routes.User.games(in.user.id, "search")
          div(cls := "dropper view-games")(
            a(href := routes.GameMod.index(in.user.id))("View", br, "Games"),
            div(cls := "view-games-dropdown")(
              a(
                cls := "fbt",
                href := s"$searchUrl?turnsMax=5&mode=1&players.loser=${in.user.id}&sort.field=d&sort.order=desc"
              )("Quick rated losses"),
              a(
                cls := "fbt",
                href := s"$searchUrl?turnsMax=5&mode=1&players.winner=${in.user.id}&sort.field=d&sort.order=desc"
              )("Quick rated wins"),
              ui.boostOpponents(in.report, in.allReports, in.user.user).map { opponents =>
                a(
                  cls  := "fbt",
                  href := s"${routes.GameMod.index(in.user.id)}?opponents=${opponents.toList.mkString(", ")}"
                )("With these opponents")
              }
            )
          )
        },
        isGranted(_.Shadowban).option(
          a(href := routes.Mod.communicationPublic(in.user.id))(
            "View",
            br,
            "Comms"
          )
        ),
        in.report.isAppeal.option(a(href := routes.Appeal.show(in.user.id))("View", br, "Appeal"))
      ),
      div(cls := "actions")(
        isGranted(_.ModMessage).option(
          div(cls := "dropper warn buttons")(
            iconTag(Icon.Envelope),
            div:
              env.mod.presets.getPmPresetsOpt.value.map: preset =>
                postForm(action := routes.Mod.warn(in.user.id, preset.name))(
                  submitButton(cls := "fbt", title := preset.text)(preset.name),
                  ui.autoNextInput
                )
          )
        ),
        isGranted(_.MarkEngine).option {
          val url = routes.Mod.engine(in.user.id, !in.user.marks.engine).url
          div(cls := "dropper engine buttons")(
            postForm(action := url, cls := "main", title := "Mark as cheat")(
              ui.markButton(in.user.marks.engine)(dataIcon := Icon.Cogs),
              ui.autoNextInput
            ),
            ui.thenForms(url, ui.markButton(false))
          )
        },
        isGranted(_.MarkBooster).option {
          val url = routes.Mod.booster(in.user.id, !in.user.marks.boost).url
          div(cls := "dropper booster buttons")(
            postForm(action := url, cls := "main", title := "Mark as booster or sandbagger")(
              ui.markButton(in.user.marks.boost)(dataIcon := Icon.LineGraph),
              ui.autoNextInput
            ),
            ui.thenForms(url, ui.markButton(false))
          )
        },
        isGranted(_.Shadowban).option {
          val url = routes.Mod.troll(in.user.id, !in.user.marks.troll).url
          div(cls := "dropper shadowban buttons")(
            postForm(
              action := url,
              title  := (if in.user.marks.troll then "Un-shadowban" else "Shadowban"),
              cls    := "main"
            )(
              ui.markButton(in.user.marks.troll)(dataIcon := Icon.BubbleSpeech),
              ui.autoNextInput
            ),
            ui.thenForms(url, ui.markButton(false))
          )
        },
        isGranted(_.CloseAccount).option {
          val url = routes.Mod.alt(in.user.id, !in.user.marks.alt).url
          div(cls := "dropper alt buttons")(
            postForm(action := url, cls := "main", title := "Close alt account")(
              ui.markButton(in.user.marks.alt)(i("A")),
              ui.autoNextInput
            ),
            ui.thenForms(url, ui.markButton(false))
          )
        },
        div(cls := "dropper more buttons")(
          iconTag(Icon.MoreTriangle),
          div(
            isGranted(_.SendToZulip).option {
              val url =
                if in.report.isAppeal then routes.Appeal.sendToZulip(in.user.username)
                else routes.Mod.inquiryToZulip
              postForm(action := url)(
                submitButton(cls := "fbt")("Send to Zulip")
              )
            },
            isGranted(_.SendToZulip).option {
              postForm(action := routes.Mod.askUsertableCheck(in.user.id))(
                submitButton(cls := "fbt")("Ask for usertable check")
              )
            },
            isGranted(_.SendToZulip).option {
              postForm(action := routes.Mod.createNameCloseVote(in.user.id))(
                submitButton(cls := "fbt")("Create name-close vote")
              )
            },
            postForm(action := routes.Report.xfiles(in.report.id))(
              submitButton(cls := List("fbt" -> true, "active" -> (in.report.room.key == "xfiles")))(
                "Move to X-Files"
              ),
              ui.autoNextInput
            ),
            div(cls := "separator"),
            lila.memo.Snooze.Duration.values.map: snooze =>
              postForm(action := snoozeUrl(in.report, snooze.toString))(
                submitButton(cls := "fbt")(s"Snooze ${snooze.name}"),
                ui.autoNextInput
              )
          )
        )
      ),
      div(cls := "actions close")(
        span(cls := "switcher", title := "Automatically open next report")(
          span(cls := "switch")(
            form3.cmnToggle("auto-next", "auto-next", checked = true)
          )
        ),
        postForm(
          action := routes.Report.process(in.report.id),
          title  := "Dismiss this report as processed. (Hotkey: d)",
          cls    := "process"
        )(
          submitButton(dataIcon := Icon.Checkmark, cls := "fbt"),
          ui.autoNextInput
        ),
        postForm(
          action := routes.Report.inquiry(in.report.id.value),
          title  := "Cancel the inquiry, re-instore the report",
          cls    := "cancel"
        )(
          submitButton(dataIcon := Icon.X, cls := "fbt")(in.alreadyMarked.option(disabled))
        )
      )
    )

  private def snoozeUrl(report: Report, duration: String): String =
    if report.isAppeal then routes.Appeal.snooze(report.user, duration).url
    else routes.Report.snooze(report.id, duration).url
