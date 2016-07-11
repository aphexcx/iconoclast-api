package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.{BSONDocument, BSONNull, BSONObjectID}
import repos.{AdRepo, ImageRepo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Ads @Inject()(val reactiveMongoApi: ReactiveMongoApi, imageRepo: ImageRepo) extends Controller
  with MongoController with ReactiveMongoComponents {

  val adRepo = AdRepo(reactiveMongoApi)

  import AdFields._

  def index = Action.async { implicit request =>
    adRepo.find() map (ads => Ok(Json.toJson(ads)))
  }

  def create = Action.async(BodyParsers.parse.json) { implicit request =>
    val adId = BSONObjectID.generate
    val imageId = BSONObjectID.generate
    // for each image url, save it in the db and get the id, then save the list of ids in the ad
    val imageIdFutures: List[Future[BSONObjectID]] = (request.body \ ImageUrls).as[List[String]].map { imageUrl =>
      imageRepo.save(BSONDocument(
        ImageFields.Id -> imageId,
        ImageFields.Url -> imageUrl,
        ImageFields.EstimatedAge -> BSONNull,
        ImageFields.AdId -> adId
      )).map(writeResult => imageId)
    }
    Future.sequence(imageIdFutures).flatMap { imageIds: List[BSONObjectID] =>
      adRepo.save(BSONDocument(
        Id -> adId,
        Url -> (request.body \ Url).as[String],
        Age -> (request.body \ Age).as[Int],
        Title -> (request.body \ Title).as[String],
        Text -> (request.body \ Text).as[String],
        ImageUrls -> imageIds
      ))
    }.map(result => Created(Json.toJson(adId)))
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
        ImageUrls -> (request.body \ ImageUrls).as[List[String]]
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
}
