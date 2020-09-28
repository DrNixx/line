package lila.gameSearch

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.game.actorApi.{ FinishGame, InsertGame }
import lila.search._
import lila.common.config._

@Module
private class GameSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("actor.name") val actorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    makeClient: Index => ESClient
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[GameSearchConfig]("gameSearch")(AutoConfig.loader)

  private lazy val client = makeClient(Index(config.indexName))

  lazy val api = new GameSearchApi(
    client,
    gameRepo
  ) // wire[GameSearchApi]

  lazy val paginator = wire[PaginatorBuilder[lila.game.Game, Query]]

  lazy val forms = wire[GameSearchForm]

  lazy val userGameSearch = wire[UserGameSearch]

  def cli =
    new lila.common.Cli {
      def process = {
        case "game" :: "search" :: username :: "index" :: Nil => api.reindex(username) inject "done"
      }
    }

  lila.common.Bus.subscribeFun("finishGame", "gameSearchInsert") {
    case FinishGame(game, _, _) if !game.aborted => api store game
    case InsertGame(game)                        => api store game
  }
}
