package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.ReadPreference
import reactivemongo.bson.{BSONDocument, BSONNull, BSONObjectID}
import repos.AdRepo

import scala.concurrent.ExecutionContext.Implicits.global

class Ads @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
  with MongoController with ReactiveMongoComponents {

  val adRepo = AdRepo(reactiveMongoApi)

  import AdFields._

  def index = Action.async { implicit request =>
    adRepo.find() map (ads => Ok(Json.toJson(ads)))
  }

  def unprocessed = Action.async { implicit request =>
    val query: BSONDocument = BSONDocument(
      EstimatedAge -> BSONNull)
    adRepo.collection.find(query)
      .cursor[BSONDocument](ReadPreference.Primary)
      .collect[List]() map (ads => Ok(Json.toJson(ads head))) //TODO change from just one ad to a stream
  }

  def create = Action.async(BodyParsers.parse.json) { implicit request =>
    adRepo.save(BSONDocument(
      Url -> (request.body \ Url).as[String],
      Age -> (request.body \ Age).as[Int],
      Title -> (request.body \ Title).as[String],
      Text -> (request.body \ Text).as[String],
      ImageUrls -> (request.body \ ImageUrls).as[List[String]],
      EstimatedAge -> BSONNull
    )).map(result => Created)
  }

  def read(id: String) = Action.async { implicit request =>
    adRepo.select(BSONDocument(id -> BSONObjectID(id)))
      .map(widget => Ok(Json.toJson(widget)))
  }

  def update(id: String) = Action.async(BodyParsers.parse.json) { implicit request =>
    adRepo.update(BSONDocument(Id -> BSONObjectID(id)),
      BSONDocument("$set" -> BSONDocument(
        Url -> (request.body \ Url).as[String],
        Age -> (request.body \ Age).as[Int],
        Title -> (request.body \ Title).as[String],
        Text -> (request.body \ Text).as[String],
        ImageUrls -> (request.body \ ImageUrls).as[List[String]],
        EstimatedAge -> (request.body \ EstimatedAge).as[Double]
      )))
      .map(result => Accepted)
  }

  def delete(id: String) = Action.async {
    adRepo.remove(BSONDocument(id -> BSONObjectID(id)))
      .map(result => Accepted)
  }

}

object AdFields {
  val Id = "_id"
  val Url = "url"
  val Age = "age"
  val Title = "title"
  val Text = "text"
  val ImageUrls = "imageUrls"
  val EstimatedAge = "estimatedAge"
}
