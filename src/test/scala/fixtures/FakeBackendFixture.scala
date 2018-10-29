package fixtures

import play.api.libs.ws.WSClient
import play.api.mvc.Result
import play.api.test.WsTestClient
import play.core.server.Server

trait FakeBackendFixture {

  def withFakeBackend(returnVal: Result, block: WSClient => Unit): Unit = {
    Server.withRouterFromComponents() { components =>
      import components.{defaultActionBuilder => Action}
    {
      case _ => Action {
        returnVal
      }
    }
    } { implicit port =>
      WsTestClient.withClient { client =>
        block(client)
      }
    }
  }
}
