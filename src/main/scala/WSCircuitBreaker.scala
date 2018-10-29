package dk.jp.circuitbreaker

import scala.concurrent.Future
import play.api.libs.ws._

trait WSCircuitBreaker extends WSRequestFilter

class WSCircuitBreakerImpl (circuitBreaker: CircuitBreaker) extends WSRequestFilter with WSCircuitBreaker {
  def apply(executor: WSRequestExecutor): WSRequestExecutor = {
    WSRequestExecutor { request =>
      monitor(() => executor(request))
    }
  }

  def monitor( executeRequest: () => Future[StandaloneWSResponse]):Future[StandaloneWSResponse] = {

    import scala.concurrent.ExecutionContext.Implicits.global
    if(circuitBreaker.allowsExecution){
      executeRequest().map{response =>
        response.status match {
          case internalServerError if 500 until 599 contains internalServerError =>
            circuitBreaker.onFailure
            throw CircuitBreakerOpenException(s"Circuit breaker failure Status code: ${response.status}, StatusText: ${response.statusText}, ResponseBody: ${response.body}")
          case _ =>
            circuitBreaker.onSuccess
            response
        }
      }.recover {
        case e: Exception =>
          circuitBreaker.onFailure
          throw CircuitBreakerOpenException("Unhandled exception from circuit breaker", e)
      }
    }
    else{
      throw CircuitBreakerOpenException(s"Circuit breaker is open ${circuitBreaker.name}")
    }
  }
}

object WSCircuitBreakerImpl{
  def apply(circuitBreaker: CircuitBreaker) = new WSCircuitBreakerImpl(circuitBreaker)
}

