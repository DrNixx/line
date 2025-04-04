package lila.mod

import chess.PlayerTitle

import lila.common.Bus
import lila.core.perm.Permission
import lila.core.report.SuspectId
import lila.core.user.{ UserMark, UserMarks }
import lila.report.{ Room, Suspect }
import lila.user.{ LightUserApi, UserRepo }

final class ModApi(
    userRepo: UserRepo,
    logApi: ModlogApi,
    reportApi: lila.report.ReportApi,
    noteApi: lila.user.NoteApi,
    prefApi: lila.core.pref.PrefApi,
    notifier: ModNotifier,
    lightUserApi: LightUserApi,
    refunder: RatingRefund
)(using Executor)
    extends lila.core.mod.ModApi:

  extension (a: UserMarks)
    def set(sel: UserMark.type => UserMark, v: Boolean) = UserMarks:
      if v then sel(UserMark) :: a.value
      else a.value.filter(sel(UserMark) !=)

  def setAlt(prev: Suspect, v: Boolean)(using me: MyId): Funit =
    for
      _ <- userRepo.setAlt(prev.user.id, v)
      sus = prev.set(_.withMarks(_.set(_.alt, v)))
      _ <- logApi.alt(sus, v)
    yield if v then notifier.reporters(me.modId, sus)

  def setEngine(prev: Suspect, v: Boolean)(using me: MyId): Funit =
    (prev.user.marks.engine != v).so {
      for
        _ <- userRepo.setEngine(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.engine, v)))
        _ <- logApi.engine(sus, v)
      yield
        Bus.publish(lila.core.mod.MarkCheater(sus.user.id, v), "adjustCheater")
        if v then
          notifier.reporters(me.modId, sus)
          refunder.schedule(sus)
    }

  def autoMark(suspectId: SuspectId, note: String)(using MyId): Funit =
    for
      sus       <- reportApi.getSuspect(suspectId.value).orFail(s"No such suspect $suspectId")
      unengined <- logApi.wasUnengined(sus)
      _ <- (!sus.user.isBot && !sus.user.marks.engine && !unengined).so {
        reportApi.getMyMod.flatMapz: mod =>
          lila.mon.cheat.autoMark.increment()
          setEngine(sus, v = true) >>
            noteApi.lichessWrite(sus.user, note) >>
            reportApi.autoProcess(sus, Set(Room.Cheat, Room.Print))
      }
    yield ()

  def setBoost(prev: Suspect, v: Boolean)(using me: Me): Fu[Suspect] =
    if prev.user.marks.boost == v then fuccess(prev)
    else
      for
        _ <- userRepo.setBoost(prev.user.id, v)
        sus = prev.set(_.withMarks(_.set(_.boost, v)))
        _ <- logApi.booster(sus, v)
      yield
        if v then
          Bus.publish(lila.core.mod.MarkBooster(sus.user.id), "adjustBooster")
          notifier.reporters(me.modId, sus)
        sus

  def setTroll(prev: Suspect, value: Boolean)(using me: MyId): Fu[Suspect] =
    if !value && prev.user.marks.isolate
    then setIsolate(prev, value).flatMap(setTroll(_, value))
    else
      val changed = value != prev.user.marks.troll
      val sus     = prev.set(_.withMarks(_.set(_.troll, value)))
      for
        _ <- changed.so:
          for _ <- userRepo.updateTroll(sus.user)
          yield
            logApi.troll(sus)
            Bus.publish(lila.core.mod.Shadowban(sus.user.id, value), "shadowban")
        _ = if value then notifier.reporters(me.modId, sus)
      yield sus

  def autoTroll(sus: Suspect, note: String): Funit =
    given MyId = UserId.lichessAsMe
    setTroll(sus, true) >>
      noteApi.lichessWrite(sus.user, note)
      >> reportApi.autoProcess(sus, Set(Room.Comm))

  def setIsolate(prev: Suspect, value: Boolean)(using me: MyId): Fu[Suspect] =
    if value && !prev.user.marks.troll
    then setTroll(prev, value).flatMap(setIsolate(_, value))
    else
      val changed = value != prev.user.marks.isolate
      val sus     = prev.set(_.withMarks(_.set(_.isolate, value)))
      for
        _ <- changed.so:
          for
            _ <- userRepo.setIsolate(sus.user.id, value)
            _ <- prefApi.isolate(sus.user)
          yield logApi.isolate(sus)
        _ = if value then notifier.reporters(me.modId, sus)
      yield sus

  def garbageCollect(userId: UserId): Funit =
    given MyId = UserId.lichessAsMe
    for
      sus <- reportApi.getSuspect(userId).orFail(s"No such suspect $userId")
      _   <- setAlt(sus, v = true)
      _   <- logApi.garbageCollect(sus)
    yield ()

  def disableTwoFactor(mod: ModId, userId: UserId): Funit =
    withUser(userId): user =>
      (userRepo.disableTwoFactor(user.id)) >> logApi.disableTwoFactor(mod, user.id)

  def reopenAccount(userId: UserId)(using Me): Funit =
    withUser(userId): user =>
      user.enabled.no.so:
        userRepo.reopen(user.id) >> logApi.reopenAccount(user.id)

  def setKid(mod: ModId, userId: UserId): Funit =
    withUser(userId): user =>
      userRepo
        .isKid(user.id)
        .flatMap: isKid =>
          (!isKid).so:
            userRepo.setKid(user, true) >> logApi.setKidMode(mod, user.id)

  def setTitle(userId: UserId, title: Option[PlayerTitle])(using Me): Funit =
    withUser(userId): user =>
      title match
        case None =>
          for
            _ <- userRepo.removeTitle(user.id)
            _ <- logApi.removeTitle(user.id)
          yield lightUserApi.invalidate(user.id)
        case Some(t) =>
          PlayerTitle.names.get(t).so { tFull =>
            for
              _ <- userRepo.setTitle(user.id, t)
              _ <- logApi.setTitle(user.id, s"$t ($tFull)")
            yield lightUserApi.invalidate(user.id)
          }

  def setEmail(userId: UserId, emailOpt: Option[EmailAddress])(using Me): Funit =
    withUser(userId): user =>
      for
        prev <- userRepo.emailOrPrevious(user.id)
        email = emailOpt | EmailAddress:
          s"noreply.blanked.${userId}${prev.fold("@nope.nope")("." + _)}"
        _ <- userRepo.setEmail(user.id, email)
        _ <- userRepo.setEmailConfirmed(user.id)
        _ <- logApi.setEmail(user.id)
      yield ()

  def setPermissions(userId: UserId, permissions: Set[Permission])(using Me): Funit =
    withUser(userId): user =>
      val finalPermissions = Permission(user).filter { p =>
        // only remove permissions the mod can actually grant
        permissions.contains(p) || !canGrant(p)
      } ++
        // only add permissions the mod can actually grant
        permissions.filter(canGrant)
      userRepo.setRoles(user.id, finalPermissions.map(_.dbKey).toList) >>
        logApi.setPermissions(user.id, permDiff(Permission(user), finalPermissions))

  private def permDiff(orig: Set[Permission], dest: Set[Permission]): Map[Permission, Boolean] = {
    orig.diff(dest).map(_ -> false) ++ dest.diff(orig).map(_ -> true)
  }.toMap

  def setReportban(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.reportban != v).so:
      userRepo.setReportban(sus.user.id, v) >> logApi.reportban(sus, v)

  def setRankban(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.rankban != v).so:
      if v then Bus.publish(lila.core.mod.KickFromRankings(sus.user.id), "kickFromRankings")
      userRepo.setRankban(sus.user.id, v) >> logApi.rankban(sus, v)

  def setArenaBan(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.arenaBan != v).so:
      userRepo.setArenaBan(sus.user.id, v) >> logApi.arenaBan(sus, v)

  def setPrizeban(sus: Suspect, v: Boolean)(using MyId): Funit =
    (sus.user.marks.prizeban != v).so:
      userRepo.setPrizeban(sus.user.id, v) >> logApi.prizeban(sus, v)

  def allMods =
    def timeNoSee(u: User): Duration = (nowMillis - (u.seenAt | u.createdAt).toMillis).millis
    userRepo
      .userIdsWithRoles(Permission.modPermissions.view.map(_.dbKey).toList)
      .flatMap(userRepo.enabledByIds)
      .map(_.sortBy(timeNoSee))

  private def withUser[A](userId: UserId)(op: User => Fu[A]): Fu[A] =
    userRepo.byId(userId).orFail(s"[mod] missing user $userId").flatMap(op)
