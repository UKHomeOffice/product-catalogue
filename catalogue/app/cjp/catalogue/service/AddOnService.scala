package cjp.catalogue.service
import cjp.catalogue.repository.ProductRepository

class AddOnService(productRepository: ProductRepository) {

  def getAllAddOnNames: List[String] = {
    "standard" :: productRepository.findAllAddOnNames
  }
}
