package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import repos.WidgetRepoImpl

import scala.concurrent.ExecutionContext.Implicits.global

class Widgets @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
  with MongoController with ReactiveMongoComponents {

  def index = Action.async { implicit request =>
    widgetRepo.find() map (widgets => Ok(Json.toJson(widgets)))
  }

  import WidgetFields._

  def create = Action.async(BodyParsers.parse.json) { implicit request =>
    val name = (request.body \ Name).as[String]
    val description = (request.body \ Description).as[String]
    val author = (request.body \ Author).as[String]
    widgetRepo.save(BSONDocument(
      Name -> name,
      Description -> description,
      Author -> author
    )).map(result => Created)
  }

  import controllers.WidgetFields._

  def read(id: String) = Action.async { implicit request =>
    widgetRepo.select(BSONDocument(id -> BSONObjectID(id)))
      .map(widget => Ok(Json.toJson(widget)))
  }

  def update(id: String) = Action.async(BodyParsers.parse.json) { implicit request =>
    val name = (request.body \ Name).as[String]
    val description = (request.body \ Description).as[String]
    val author = (request.body \ Author).as[String]
    widgetRepo.update(BSONDocument(Id -> BSONObjectID(id)),
      BSONDocument("$set" -> BSONDocument(Name -> name, Description -> description, Author -> author)))
      .map(result => Accepted)
  }

  def delete(id: String) = Action.async {
    widgetRepo.remove(BSONDocument(id -> BSONObjectID(id)))
      .map(result => Accepted)
  }

  def widgetRepo = new WidgetRepoImpl(reactiveMongoApi)

}

object WidgetFields {
  val Id = "_id"
  val Name = "name"
  val Description = "description"
  val Author = "author"
}
