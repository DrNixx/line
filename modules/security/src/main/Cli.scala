package lila.security

import lila.user.{ User, UserRepo }

private[security] final class Cli extends lila.common.Cli {

  def process = {

    case "security" :: "roles" :: uid :: Nil =>
      UserRepo byId uid map {
        _.fold("User %s not found" format uid)(_.roles mkString " ")
      }

    case "security" :: "grant" :: uid :: roles =>
      perform(uid, user =>
        UserRepo.setRoles(user.id, roles map (_.toUpperCase)).void)
  }

  private def perform(userId: User.ID, op: User => Funit): Fu[String] =
    UserRepo byId userId flatMap { userOption =>
      userOption.fold(fufail[String]("User %s not found" format userId)) { u =>
        op(u) inject "User %s successfully updated".format(u.username)
      }
    }
}
