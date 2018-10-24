package repositories
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import play.api.libs.ws._

trait Gauge{
def set(value:Double)
}

case class CircuitBreakerGauges(successfulCount: Gauge, failedCount: Gauge)

case class CircuitBreakerOpenException(private val message: String = "", ex: Exception = null)
    extends Exception(message, ex)


trait WSCircuitBreaker extends WSRequestFilter


class WSCircuitBreakerImpl (circuitBreaker: CircuitBreaker) extends WSRequestFilter with WSCircuitBreaker {
  def apply(executor: WSRequestExecutor): WSRequestExecutor = {
    WSRequestExecutor { request =>
      Monitor(() => executor(request))
    }
  }

  def Monitor( executeRequest: () => Future[StandaloneWSResponse]):Future[StandaloneWSResponse] = {

    import scala.concurrent.ExecutionContext.Implicits.global
    if(circuitBreaker.allowsExecution){
      executeRequest().map{response =>
        response.status match {
          case internalServerError if 500 until 599 contains internalServerError =>
              println(s"Circuit breaker failure Status code: ${response.status}, StatusText: ${response.statusText}, ResponseBody: ${response.body}")
            circuitBreaker.onFailure
            throw new CircuitBreakerOpenException(s"Circuit breaker failure Status code: ${response.status}, StatusText: ${response.statusText}, ResponseBody: ${response.body}")
          case _ =>
            circuitBreaker.onSuccess
            response
        }
      }.recover {
        case e: Exception =>
          circuitBreaker.onFailure
          println("WS circuit breaker failure", e)
          throw new CircuitBreakerOpenException("Unhandled exception from circuit breaker", e)
      }
    }
    else{
      throw new CircuitBreakerOpenException(s"the circuit breaker is open ${circuitBreaker.name}")
    }
  }
}

object WSCircuitBreakerImpl{
  def apply(circuitBreaker: CircuitBreaker) = new WSCircuitBreakerImpl(circuitBreaker)
}



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
