import org.scalatestplus.play._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with HtmlUnitFactory {

  //  override lazy val port = 9000

  "Application" should {

    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include("Your database is ready.")
    }

    "remove data through the browser" in {

      go to ("http://localhost:" + port + "/cleanup")

      pageSource must include("Your database is clean.")
    }
  }
}


