package fixtures

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

trait TestFutureFixture extends ScalaFutures {

  protected val patienceTimeoutInSeconds = 10L
  protected val patienceIntervalSpanInMillis = 30L
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(patienceTimeoutInSeconds, Seconds),
    interval = Span(patienceIntervalSpanInMillis, Millis))
}
