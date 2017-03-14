package cjp.catalogue.repository

case class ProductNameAlreadyUsedException(name: String) extends RuntimeException(s"$name  has already been used")