package lila.message

import play.api.data._
import play.api.data.Forms._

import lila.security.Granter
import lila.user.{ User, UserRepo }

private[message] final class DataForm(security: MessageSecurity) {

  import DataForm._

  def thread(me: User) = Form(mapping(
    "userid" -> nonEmptyText(maxLength = 20)
      .verifying("Unknown user", { fetchUser(_).isDefined })
      .verifying("Sorry, this player doesn't accept new messages", { userId =>
        Granter(_.MessageAnyone)(me) || {
          security.canMessage(me.id, userId) awaitSeconds 2 // damn you blocking API
        }
      }),
    "subject" -> text(minLength = 3, maxLength = 100),
    "text" -> text(minLength = 3, maxLength = 8000),
    "mod" -> optional(nonEmptyText)
  )({
      case (userid, subject, text, mod) => ThreadData(
        user = fetchUser(userid) err "Unknown user " + userid,
        subject = subject,
        text = text,
        asMod = mod.isDefined
      )
    })(_.export.some))

  def post = Form(single(
    "text" -> text(minLength = 3)
  ))

  private def fetchUser(userid: String) = UserRepo byId userid awaitSeconds 2
}

object DataForm {

  case class ThreadData(
      user: User,
      subject: String,
      text: String,
      asMod: Boolean
  ) {

    def export = (user.id, subject, text, asMod option "1")
  }
}
