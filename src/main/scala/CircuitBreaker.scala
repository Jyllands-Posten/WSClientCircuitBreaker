package dk.jp.circuitbreaker
import scala.concurrent.duration.FiniteDuration

case class CircuitBreakerOpenException(private val message: String = "", ex: Exception = null)
    extends Exception(message, ex)

object CircuitBreakerState extends Enumeration {
  type CircuitBreakerState = Value
  val Open = Value("Open")
  val HalfOpen = Value("HalfOpen")
  val Closed = Value("Closed")
}

case class CircuitBreakerConfig(val name: String, val retryDelay: FiniteDuration, val successThreshold:Int, val failThreshold:Int)


trait CircuitBreaker {
  val name: String
  def onSuccess
  def onFailure
  def allowsExecution: Boolean

}
// ok so this is completely a java thing stateful and all

class CircuitBreakerImpl (config: CircuitBreakerConfig, gauges: CircuitBreakerGauges) extends CircuitBreaker{
  val name: String = config.name
  val retryDelay: FiniteDuration = config.retryDelay
  val failThreshold:Int = config.failThreshold
  val successThreshold:Int = config.successThreshold
  
  
  var state: CircuitBreakerState.CircuitBreakerState = CircuitBreakerState.Closed
  var retryAt: Long = 0
  var successfulCount:Double= 0
  var failedCount:Double = 0

  def reportToPrometheus(){
    gauges.successfulCount.set(successfulCount)
    gauges.failedCount.set(failedCount)
  }


  def onSuccess(){
    successfulCount = successfulCount + 1
    failedCount = 0
    if(state == CircuitBreakerState.Open && successfulCount >= successThreshold){
      state = CircuitBreakerState.Closed
    }
    reportToPrometheus()
  }

  def onFailure(){
    println("circuit breaker failure")
    failedCount = failedCount + 1
    successfulCount = 0

    if(state == CircuitBreakerState.HalfOpen){
      state = CircuitBreakerState.Open
      retryAt = System.currentTimeMillis() + retryDelay.toMillis
    }
    if(failedCount >= failThreshold){
      state = CircuitBreakerState.Open
      retryAt = System.currentTimeMillis() + retryDelay.toMillis
    }

    reportToPrometheus()
  }

  def allowsExecution(): Boolean = {
    if(state == CircuitBreakerState.Closed){
      return true
    }
    if(state == CircuitBreakerState.HalfOpen){
      return true
    }
    if (state == CircuitBreakerState.Open && System.currentTimeMillis()>= retryAt ){
      state = CircuitBreakerState.HalfOpen
      return true
    }

    println(s"circuit breaker is not allowing execution since circuit is open name: ${name}")
    return false
  }
}
