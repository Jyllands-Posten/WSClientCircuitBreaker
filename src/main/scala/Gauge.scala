package dk.jp.circuitbreaker
trait Gauge{
def set(value:Double)
}

case class CircuitBreakerGauges(successfulCount: Gauge, failedCount: Gauge)

