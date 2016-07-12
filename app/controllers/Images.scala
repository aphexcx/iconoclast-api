package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.ReadPreference
import reactivemongo.bson.{BSONDocument, BSONNull, BSONObjectID}
import repos.ImageRepo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Images @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
  with MongoController with ReactiveMongoComponents {

  val imageRepo = ImageRepo(reactiveMongoApi)

  import ImageFields._

  def index = Action.async { implicit request =>
    imageRepo.find() map (images => Ok(Json.toJson(images)))
  }

  def underage = Action.async { implicit request =>
    val query: BSONDocument = BSONDocument(
      EstimatedAge -> BSONDocument(
        "$lt" -> 21.0,
        "$ne" -> -1.0
      )
    )
    imageRepo.collection.find(query)
      //      .sort(BSONDocument(EstimatedAge -> -1))
      .cursor[BSONDocument](ReadPreference.Primary)
      .collect[List]()
      //      .flatMap(option => option.map(lock).getOrElse(Future(BSONDocument())))
      .map(imageList => Ok(Json.toJson(imageList)))
  }

  def unprocessed = Action.async { implicit request =>
    val query: BSONDocument = BSONDocument(
      EstimatedAge -> BSONNull,
      LockUntil -> BSONDocument("$lt" -> System.currentTimeMillis)
    )
    imageRepo.collection.find(query).one[BSONDocument]
      .flatMap(option => option.map(lock).getOrElse(Future(BSONDocument())))
      .map(image => Ok(Json.toJson(image)))
  }

  def lock(image: BSONDocument): Future[BSONDocument] = {
    imageRepo.update(BSONDocument(Id -> image.getAs[BSONObjectID]("_id").get),
      BSONDocument("$set" -> BSONDocument(
        LockUntil -> (System.currentTimeMillis + 60 * 1000)
      ))) map (writeResult => image)
  }

  def create = Action.async(BodyParsers.parse.json) { implicit request =>
    imageRepo.save(BSONDocument(
      Url -> (request.body \ Url).as[String],
      EstimatedAge -> (request.body \ EstimatedAge).as[Double],
      AdId -> (request.body \ AdId).as[BSONObjectID],
      LockUntil -> 0
    )).map(result => Created(Json.toJson(result.originalDocument.get))) //TODO excise .get
    //TODO if originalDocument doesnt work, use another method to return the id
  }

  def read(id: String) = Action.async { implicit request =>
    imageRepo.select(BSONDocument(id -> BSONObjectID(id)))
      .map(widget => Ok(Json.toJson(widget)))
  }

  def update(id: String) = Action.async(BodyParsers.parse.json) { implicit request =>
    imageRepo.update(BSONDocument(Id -> BSONObjectID(id)),
      BSONDocument("$set" -> BSONDocument(
        Url -> (request.body \ Url).as[String],
        EstimatedAge -> (request.body \ EstimatedAge).as[Double],
        AdId -> (request.body \ AdId).as[BSONObjectID],
        LockUntil -> 0
      )))
      .map(result => Accepted)
  }
}


object ImageFields {
  val Id = "_id"
  val Url = "url"
  val EstimatedAge = "estimatedAge"
  val AdId = "adId"
  val LockUntil = "lockUntil"
}