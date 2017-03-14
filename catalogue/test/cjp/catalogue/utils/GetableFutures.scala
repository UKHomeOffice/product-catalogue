package cjp.catalogue.utils

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object GetableFutures {
  implicit class GetHelper[T](val inContextFuture: Future[T]) extends AnyVal {
    def await(): T = get(5 seconds)
    def get(duration: Duration): T = Await result (inContextFuture, duration)
    def get: T = Await result (inContextFuture, 2 seconds)
  }
}
