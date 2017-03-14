import cjp.catalogue.filter.AccessLogFilter
import play.api.mvc.WithFilters


object Global extends WithFilters(AccessLogFilter) {
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    ComponentRegistry.getController(controllerClass)
  }
}
