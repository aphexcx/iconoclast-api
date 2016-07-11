import controllers.Ads
import org.junit.runner.RunWith
import org.scalatestplus.play._
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Result, _}
import play.api.test.Helpers._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.LastError
import reactivemongo.bson.BSONDocument
import repos.{AdRepo, ImageRepo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpec with OneAppPerTest with Results with Mockito {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  val mockAdRepo = mock[AdRepo]
  val mockImageRepo = mock[ImageRepo]
  val reactiveMongoApi = mock[ReactiveMongoApi]
  val documentId = "56a0ddb6c70000c700344254"
  val lastRequestStatus = new LastError(true, None, None, None, 0, None, false, None, None, false, None, None)

  val oatmealStout = Json.obj(
    "name" -> "Widget One",
    "description" -> "My first widget",
    "author" -> "Justin"
  )

  val posts = List(
    oatmealStout,
    Json.obj(
      "name" -> "Widget Two: The Return",
      "description" -> "My second widget",
      "author" -> "Justin"
    ))
  val controller = new TestController()

  class TestController() extends Ads(reactiveMongoApi, mockImageRepo) {
    override val adRepo: AdRepo = mockAdRepo
  }


  "Recipes#delete" should {
    "remove recipe" in {
      mockAdRepo.remove(any[BSONDocument])(any[ExecutionContext]) returns Future(lastRequestStatus)

      val result: Future[Result] = controller.delete(documentId).apply(FakeRequest())

      status(result) mustEqual ACCEPTED
      there was one(mockAdRepo).remove(any[BSONDocument])(any[ExecutionContext])
    }
  }

  "Recipes#list" should {
    "list recipes" in {
      mockAdRepo.find()(any[ExecutionContext]) returns Future(posts)

      val result: Future[Result] = controller.index().apply(FakeRequest())

      contentAsJson(result) mustEqual JsArray(posts)
      there was one(mockAdRepo).find()(any[ExecutionContext])
    }
  }

  "Recipes#read" should {
    "read recipe" in {
      mockAdRepo.select(any[BSONDocument])(any[ExecutionContext]) returns Future(Option(oatmealStout))

      val result: Future[Result] = controller.read(documentId).apply(FakeRequest())

      contentAsJson(result) mustEqual oatmealStout
      there was one(mockAdRepo).select(any[BSONDocument])(any[ExecutionContext])
    }
  }

  "Recipes#create" should {
    "create recipe" in {
      mockAdRepo.save(any[BSONDocument])(any[ExecutionContext]) returns Future(lastRequestStatus)

      val request = FakeRequest().withBody(oatmealStout)
      val result: Future[Result] = controller.create()(request)

      status(result) mustEqual CREATED
      there was one(mockAdRepo).save(any[BSONDocument])(any[ExecutionContext])
    }
  }

  "Recipes#update" should {
    "update recipe" in {
      mockAdRepo.update(any[BSONDocument], any[BSONDocument])(any[ExecutionContext]) returns Future(lastRequestStatus)

      val request = FakeRequest().withBody(oatmealStout)
      val result: Future[Result] = controller.update(documentId)(request)

      status(result) mustEqual ACCEPTED
      there was one(mockAdRepo).update(any[BSONDocument], any[BSONDocument])(any[ExecutionContext])
    }
  }

  "HomeController" should {

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/plain")
      contentAsString(home) must include("Your database is ready.")
    }

  }

  "CountController" should {

    "return an increasing count" in {
      contentAsString(route(app, FakeRequest(GET, "/count")).get) mustBe "0"
      contentAsString(route(app, FakeRequest(GET, "/count")).get) mustBe "1"
      contentAsString(route(app, FakeRequest(GET, "/count")).get) mustBe "2"
    }

  }

}
