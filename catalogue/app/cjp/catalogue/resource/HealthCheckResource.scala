package cjp.catalogue.resource


import cjp.catalogue.mongo.MongoConnector
import cjp.catalogue.util.BuildInfo
import play.api.mvc.{Action, Controller, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class HealthCheckResource(mongoHealthCheck: MongoHealthCheck, buildInfo: Option[BuildInfo]) extends Controller {

  def healthCheck = Action.async {
    request =>
      mongoHealthCheck.ping() map {
        case ok if ok => Ok(healthMessage())
        case _ => Results.InternalServerError(healthMessage("unhealthy! Mongo reported a negative response to ping!"))
      }
  }

  private def healthMessage(report:String = "healthy!") = {
    val buildNumber = buildInfo.map(_.buildNumber).getOrElse("[not available]")
    s"""|$report
       |build-number: $buildNumber""".stripMargin
  }

  def ping = Action {
    request => Results.Ok("pong")
  }
}


class MongoHealthCheck(mongo: MongoConnector) {

  def ping(): Future[Boolean] = {
    Try(mongo.db()("any").size) match {
      case Success(_) => Future.successful(true)
      case Failure(e) => Future.successful(false)
    }
  }
}