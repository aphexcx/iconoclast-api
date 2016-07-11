package repos

import javax.inject.Inject

import controllers.AdFields
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}

import scalaz.syntax.id._

/**
  * Created by aphex on 7/10/16.
  */
class ImageRepo @Inject()(reactiveMongoApi: ReactiveMongoApi) extends Repo(reactiveMongoApi: ReactiveMongoApi) {
  override val collectionName: String = "images"
}

object ImageRepo {

  def apply(reactiveMongoApi: ReactiveMongoApi): ImageRepo = new ImageRepo(reactiveMongoApi) <| setupIndex

  def setupIndex(repo: ImageRepo): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    repo.collection.indexesManager.ensure(Index(key = Seq(AdFields.Url -> IndexType.Text), unique = true)) onComplete { r =>
      println(s"db: $r when ensuring unique index on key $AdFields.Url")
    }
  }
}