import dk.jp.circuitbreaker._
import fixtures.{FakeBackendFixture, TestFutureFixture}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.mvc.Results

import scala.concurrent.duration._

class WSCircuitBreakerSpec extends PlaySpec with TestFutureFixture with FakeBackendFixture {

  val successThreshold = 5
  val failThreshold = 5
  val circuitBreakerConfig = CircuitBreakerConfig("Circuit breaker", 2.seconds, successThreshold , failThreshold)
  val gauges = CircuitBreakerGauges(TestGauge(0), TestGauge(0))
  val circuitBreaker = WSCircuitBreakerImpl(new CircuitBreakerImpl(circuitBreakerConfig, gauges))


  "WSCircuitBreaker" should {
    "Increment successfulCount" when {
      "Status code is 200" in {
        withFakeBackend(Results.Ok, { client =>
          val request = client.url("/")
            .withRequestFilter(circuitBreaker)
            .execute()

          whenReady(request){ result =>
            result.status mustBe OK
            gauges.failedCount.get() mustBe 0
            gauges.successfulCount.get() mustBe 1
          }
        })
      }
    }

    "Throw CircuitBreakerOpen exception" when {
      "Status code is 5xx" in {
        withFakeBackend(Results.InternalServerError, { client =>
          val request = client.url("/")
            .withRequestFilter(circuitBreaker)
            .execute()

          whenReady(request.failed){ exception =>
            exception mustBe a[CircuitBreakerOpenException]
          }
        })
      }
    }

    "retry until fail threshold is reached" when {
      "Status code is 5xx" in {
        withFakeBackend(Results.InternalServerError, { client =>
          val request = client.url("/")
            .withRequestFilter(circuitBreaker)
            .execute()

          whenReady(request.failed){ _=>
            gauges.successfulCount.get() mustBe 0
            gauges.failedCount.get() mustBe failThreshold
          }
        })
      }
    }
  }

  case class TestGauge(var value: Double) extends Gauge {
    override def set(value: Double): Unit = { this.value = value}
    override def get(): Double = { value}
  }
}
