package lila.blog

object ProtocolFix {

  private val RemoveRegex = """http://(\w{2}\.)?+live\.chess-online\.com""".r
  def remove(html: String) = RemoveRegex.replaceAllIn(html, _ => "//live.chess-online.com")

  private val AddRegex = """(https?+:)?+(//)?+(\w{2}\.)?+live\.chess-online\.org""".r
  def add(html: String) = AddRegex.replaceAllIn(html, _ => "https://live.chess-online.com")
}
