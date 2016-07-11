package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import repos.ImageRepo

import scala.concurrent.ExecutionContext.Implicits.global

class Images @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
  with MongoController with ReactiveMongoComponents {

  val imageRepo = ImageRepo(reactiveMongoApi)

  import ImageFields._

  def index = Action.async { implicit request =>
    imageRepo.find() map (images => Ok(Json.toJson(images)))
  }

  def create = Action.async(BodyParsers.parse.json) { implicit request =>

    imageRepo.save(BSONDocument(
      Url -> (request.body \ Url).as[String],
      EstimatedAge -> (request.body \ EstimatedAge).as[Double],
      AdId -> BSONObjectID((request.body \ AdId).as[Array[Byte]])
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
        AdId -> BSONObjectID((request.body \ AdId).as[Array[Byte]])
      )))
      .map(result => Accepted)
  }
}


object ImageFields {
  val Id = "_id"
  val Url = "url"
  val EstimatedAge = "estimatedAge"
  val AdId = "ad"
}