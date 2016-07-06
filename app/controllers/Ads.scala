package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import repos.AdRepoImpl

import scala.concurrent.ExecutionContext.Implicits.global

class Ads @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
  with MongoController with ReactiveMongoComponents {

  def index = Action.async { implicit request =>
    adRepo.find() map (ads => Ok(Json.toJson(ads)))
  }

  import AdFields._

  def create = Action.async(BodyParsers.parse.json) { implicit request =>
    val age = (request.body \ Age).as[Int]
    val title = (request.body \ Title).as[String]
    val text = (request.body \ Text).as[String]
    val imageUrls = (request.body \ ImageUrls).as[List[String]]
    adRepo.save(BSONDocument(
      ImageUrls -> imageUrls,
      Age -> age,
      Title -> title,
      Text -> text
    )).map(result => Created)
  }

  import controllers.AdFields._

  def read(id: String) = Action.async { implicit request =>
    adRepo.select(BSONDocument(id -> BSONObjectID(id)))
      .map(widget => Ok(Json.toJson(widget)))
  }

  def update(id: String) = Action.async(BodyParsers.parse.json) { implicit request =>
    val age = (request.body \ Age).as[Int]
    val title = (request.body \ Title).as[String]
    val text = (request.body \ Text).as[String]
    val imageUrls = (request.body \ ImageUrls).as[List[String]]
    adRepo.update(BSONDocument(Id -> BSONObjectID(id)),
      BSONDocument("$set" -> BSONDocument(
        ImageUrls -> imageUrls,
        Age -> age,
        Title -> title,
        Text -> text
      )))
      .map(result => Accepted)
  }

  def delete(id: String) = Action.async {
    adRepo.remove(BSONDocument(id -> BSONObjectID(id)))
      .map(result => Accepted)
  }

  def adRepo = new AdRepoImpl(reactiveMongoApi)

}

object AdFields {
  val Id = "_id"
  val Age = "age"
  val Title = "title"
  val Text = "text"
  val ImageUrls = "imageUrls"
}
