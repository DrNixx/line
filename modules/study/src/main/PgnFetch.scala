package lila.study

import play.api.libs.ws.WS
import play.api.Play.current

private final class PgnFetch {

  private type Pgn = String
  private val pgnContentType = "application/x-chess-pgn"

  // http://www.chessgames.com/perl/chessgame?gid=1427487
  // http://www.chessgames.com/perl/nph-chesspgn?text=1&gid=1427487
  // http://www.chessgames.com/pgn/boleslavsky_ufimtsev_1944.pgn?gid=1427487
  private val ChessbaseRegex = """.*chessgames\.com/.*[\?&]gid=(\d+).*""".r

  // https://www.chess-online.com/ru-ru/7697348
  // https://backend.chess-online.com/ru-ru/arbiter/game/7697348
  // https://backend.chess-online.com/ru-ru/arbiter/game?id=7697348
  private val Chessonline1Regex = """https://www\.chess-online\.com/[a-z]{2,3}(?:-[a-z]{2,3})?/(\d+)""".r
  private val Chessonline2Regex = """https://backend\.chess-online\.com/[a-z]{2,3}(?:-[a-z]{2,3})?/arbiter/game(?:/|\?id=)(\d+)""".r

  def fromUrl(url: String): Fu[Option[Pgn]] = url match {
    case Chessonline1Regex(id) => parseIntOption(id) ?? downloadChessonline
    case Chessonline2Regex(id) => parseIntOption(id) ?? downloadChessonline
    case ChessbaseRegex(id) => parseIntOption(id) ?? downloadChessbase
    case _ => fuccess(none)
  }

  private def downloadChessbase(id: Int): Fu[Option[Pgn]] = {
    WS.url(s"""http://www.chessgames.com/pgn/any.pgn?gid=$id""").get().map { res =>
      res.header("Content-Type").contains(pgnContentType) option res.body
    }
  }

  private def downloadChessonline(id: Int): Fu[Option[Pgn]] = {
    WS.url(s"""https://www.chess-online.com/chess/pgn/$id""").get().map { res =>
      res.header("Content-Type").contains(pgnContentType) option res.body
    }
  }
}
