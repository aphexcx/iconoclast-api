package repos

import javax.inject.Inject

import controllers.AdFields
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}
import scalaz.syntax.id._

trait AdRepo {
  def find()(implicit ec: ExecutionContext): Future[List[JsObject]]

  def select(selector: BSONDocument)(implicit ec: ExecutionContext): Future[Option[JsObject]]

  def update(selector: BSONDocument, update: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult]

  def remove(document: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult]

  def save(document: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult]
}

class AdRepoImpl @Inject()(reactiveMongoApi: ReactiveMongoApi) extends AdRepo {

  override def find()(implicit ec: ExecutionContext): Future[List[JsObject]] = {
    val genericQueryBuilder = collection.find(Json.obj())
    val cursor = genericQueryBuilder.cursor[JsObject](ReadPreference.Primary)
    cursor.collect[List](100)
  }

  def collection: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("ads")

  override def select(selector: BSONDocument)(implicit ec: ExecutionContext): Future[Option[JsObject]] = {
    collection.find(selector).one[JsObject]
  }

  override def update(selector: BSONDocument, update: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult] = {
    collection.update(selector, update)
  }

  override def remove(document: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult] = {
    collection.remove(document)
  }

  override def save(document: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult] = {
    collection.update(BSONDocument("_id" -> document.get("_id").getOrElse(BSONObjectID.generate)), document, upsert = true)
  }

}

object AdRepoImpl {

  def apply(reactiveMongoApi: ReactiveMongoApi): AdRepoImpl = new AdRepoImpl(reactiveMongoApi) <| setupIndex

  def setupIndex(impl: AdRepoImpl): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    impl.collection.indexesManager.ensure(Index(key = Seq(AdFields.Url -> IndexType.Text), unique = true)) onComplete { r =>
      println(s"db: $r when ensuring unique index on key $AdFields.Url")
    }
  }
}