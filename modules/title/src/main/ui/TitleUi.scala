package lila.title
package ui

import chess.PlayerTitle
import play.api.data.Form

import lila.core.id.ImageId
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TitleUi(helpers: Helpers)(picfitUrl: lila.core.misc.PicfitUrl):
  import helpers.{ *, given }

  def index(page: Page, intro: Frag)(using Context) =
    page.css("bits.page"):
      frag(
        intro,
        br,
        br,
        br,
        div(style := "text-align: center;")(
          a(cls := "button button-fat", href := routes.TitleVerify.form)("Проверить моё звание")
        )
      )

  def create(page: Page, form: Form[TitleRequest.FormData])(using Context) =
    page:
      frag(
        h1(cls := "box__top")(page.title),
        postForm(cls := "form3", action := routes.TitleVerify.create)(
          dataForm(form),
          form3.action(form3.submit("Далее"))
        )
      )

  def edit(page: Page, form: Form[TitleRequest.FormData], req: TitleRequest)(using Context) =
    page
      .js(esmInitBit("titleRequest")):
        frag(
          h1(cls := "box__top")(page.title),
          standardFlash,
          showStatus(req),
          if req.status.is(_.approved)
          then a(href := routes.TitleVerify.form)("Новый запрос на проверку")
          else if req.status.is(_.rejected)
          then emptyFrag
          else showForms(req, form)
        )

  private def showForms(req: TitleRequest, form: Form[TitleRequest.FormData])(using Context) =
    frag(
      div(cls := "title__images")(
        imageByTag(
          req,
          "idDocument",
          name = "Документ",
          help = div(
            p("Паспорт или водительское удостоверение."),
            p(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB."))
          )
        ),
        imageByTag(
          req,
          "selfie",
          name = "Ваше изображение",
          help = div(
            p("""Фотография, на которой вы держите лист бумаги с требуемым текстом:"""),
            pre("""Официальная проверка Chess-Online
Мой аккаунт на Chess-Online: [ваше имя пользователя или ID]
Сегодняшняя дата: [текущая дата]""")
          )
        )
      ),
      postForm(cls := "form3", action := routes.TitleVerify.update(req.id))(
        dataForm(form),
        form3.action(form3.submit("Обновить и отправить на проверку"))
      ),
      postForm(cls := "form3", action := routes.TitleVerify.cancel(req.id))(
        form3.action(
          form3.submit("Отменить запрос и удалить данные формы", icon = Icon.Trash.some)(
            cls := "button-red button-empty yes-no-confirm"
          )
        )(cls := "title__cancel")
      )
    )

  private def showStatus(req: TitleRequest)(using Context) =
    import TitleRequest.Status
    div(cls := "title__status-full")(
      statusFlair(req),
      div(cls := "title__status__body")(
        req.status match
          case Status.building =>
            frag("Пожалуйста, загрузите необходимые документы для подтверждения вашей личности.")
          case Status.pending(_) =>
            frag(
              h2("Все готово! Ваш запрос находится на рассмотрении."),
              "Модератор рассмотрит его в ближайшее время. Вы получите сообщение Chess-Online, как только оно будет обработано."
            )
          case Status.approved =>
            h2("Ваше ", nbsp, userTitleTag(req.data.title), nbsp, " звание было подтверждено!")
          case Status.rejected    => h2("Ваш запрос был отклонен.")
          case Status.imported    => h2("Ваш запрос был заархивирован.")
          case Status.feedback(t) => frag("Комментарии модератора:", br, br, strong(t))
      )
    )

  def statusFlair(req: TitleRequest)(using Context) = iconFlair:
    Flair:
      req.status.name match
        case "approved" => "activity.sparkles"
        case "rejected" => "symbols.cross-mark"
        case "feedback" => "symbols.speech-balloon"
        case "imported" => "objects.books"
        case _          => "objects.hourglass-not-done"

  private def dataForm(form: Form[TitleRequest.FormData])(using Context) =
    frag(
      form3.globalError(form),
      form3.split(
        form3.group(
          form("title"),
          "Title"
        ): field =>
          form3.select(
            field,
            availableTitles.map(t => t -> t.value),
            default = "Выберите Ваше звание".some
          ),
        form3.group(
          form("realName"),
          "Полное настоящее ФИО",
          half = true
        )(form3.input(_)(autofocus))
      ),
      form3.split(
        form3.group(
          form("fideId"),
          "Ваш идентификатор ФИДЕ или URL-адрес профиля",
          help = frag("Если есть.").some,
          half = true
        )(form3.input(_)),
        form3.group(
          form("federationUrl"),
          "URL-адрес вашего профиля в национальной федерации",
          help = frag("Если есть.").some,
          half = true
        )(form3.input(_))
      ),
      form3.split(
        form3.checkbox(
          if form("public").value.isDefined || form.hasErrors
          then form("public")
          else form("public").copy(value = "true".some),
          frag("Публичный аккаунт"),
          help = frag(
            "Сделать Ваше настоящее имя публичным. Требуется для тренеров, стриминга и призовых турниров."
          ).some,
          half = true
        ),
        form3.checkbox(
          form("coach"),
          frag("Создать профиль тренера"),
          help = frag(
            "Предложите свои услуги в качестве тренера и появитесь в ",
            a(href := routes.Coach.all())("списке тренеров"),
            "."
          ).some,
          half = true
        )
      ),
      form3.group(
        form("comment"),
        "Комментарии",
        help = frag("Необязательная дополнительная информация для модераторов.").some,
        half = true
      )(form3.textarea(_)(rows := 4))
    )

  private def imageByTag(t: TitleRequest, tag: String, name: Frag, help: Frag)(using ctx: Context) =
    val image = t.focusImage(tag).get
    div(cls := "title-image-edit", data("post-url") := routes.TitleVerify.image(t.id, tag))(
      h2(name),
      thumbnail(image, 200)(
        cls               := List("drop-target" -> true, "user-image" -> image.isDefined),
        attr("draggable") := "true"
      ),
      help
    )

  object thumbnail:
    def apply(image: Option[ImageId], height: Int): Tag =
      image.fold(fallback): id =>
        img(cls := "title-image", src := url(id, height))
    def fallback                      = iconTag(Icon.UploadCloud)(cls := "title-image--fallback")
    def url(id: ImageId, height: Int) = picfitUrl.resize(id, Right(height))
    def raw(id: ImageId)              = picfitUrl.raw(id)
