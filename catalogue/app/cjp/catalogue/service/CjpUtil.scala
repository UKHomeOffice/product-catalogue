package cjp.catalogue.service

object CjpUtil {

  def buildId(name: String) = {
    name.trim().toLowerCase().replaceAll("\\W+", "-")
  }

}
