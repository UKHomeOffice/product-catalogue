package cjp.catalogue.service

import cjp.catalogue.model.{CatalogueAttributeDefinition, CatalogueBigDecimalProductAttribute, CatalogueBooleanProductAttribute, CatalogueIntegerProductAttribute, NewCatalogueProduct, PersistedCatalogueProduct, _}
import cjp.catalogue.repository.{ProductRepository, ProductTimelineRepository}
import cjp.catalogue.utils.Logging
import com.mongodb.casbah.Imports
import org.joda.time.DateTimeZone._
import org.joda.time.{DateTime, LocalDate, LocalTime}


class ProductNotFoundException(productName: String) extends RuntimeException(s"Product [$productName] not found")

class NoSuchVersionException(productName: String, version: Int) extends RuntimeException(s"Version [$version] not found for product $productName")

class ProductManagementService(productRepository: ProductRepository,
                               productTimelineRepository: ProductTimelineRepository,
                               attributeService: AttributeService) extends AttributeMapper with Logging {


  type AttributeFinder = (String, String) => Option[CatalogueAttributeDefinition]

  def getCurrent(name: String): Option[ProductDto] =
    getEffectiveAt(name, DateTime.now)

  def getEffectiveAt(name: String, effectiveAt: DateTime): Option[ProductDto] =
    findEffectiveProductAndFromDate(name, effectiveAt).map(productAndEffectiveFrom => asProductDto(productAndEffectiveFrom._1, productAndEffectiveFrom._2))

  def getDraft(name: String): Option[ProductDto] =
    findDraftAndFromDate(name).map(productAndEffectiveFrom => asProductDto(productAndEffectiveFrom._1, productAndEffectiveFrom._2))

  def create(productDto: ProductDto): String = {
    val name = CjpUtil.buildId(productDto.title)
    val productVersion = verifyAndConvert(productDto.copy(name = Some(name)))
    val newProduct = productRepository.createNewProduct(productVersion)
    val effectiveFrom = toDateTime(productDto.effectiveFrom)
    productTimelineRepository.createNewTimeline(newProduct.name)
    if(effectiveFrom.isAfterNow)
      productTimelineRepository.setDraftProduct(newProduct.name, newProduct.version, effectiveFrom)
    else
      productTimelineRepository.setActiveProduct(newProduct.name, newProduct.version, effectiveFrom)
    name
  }

  def cloneProduct(existingProductName: String, product: CloneProductDto): Option[String] = {
    getCurrent(existingProductName) map { existing =>
      create(existing.copy(name = None, title = product.title, description = product.description, version = None, effectiveFrom = product.effectiveFrom))
    }
  }

  private def toDateTime(localDate: LocalDate) = localDate.toDateTime(new LocalTime(0, 0), UTC)

  def update(name: String, productDto: ProductDto): Unit = {

    val effectiveFrom = toDateTime(productDto.effectiveFrom)

    if (effectiveFrom.plusDays(1).toDateMidnight.isBeforeNow) {
      throw new IllegalStateException(s"Cannot update product [$name] - effective date [${productDto.effectiveFrom}] must be at least one day ahead from now")
    }

    findDraftByName(name) match {
      case Some(draft) =>
        val newDraft = verifyAndConvert(productDto, draft.created)
        productRepository.update(draft._id, draft.version, newDraft)
        productTimelineRepository.updateDraftProduct(draft.name, draft.version, effectiveFrom)

      case None =>
        val activeProduct = findEffectiveProduct(name).getOrElse(throw new ProductNotFoundException(name))
        val pv = verifyAndConvert(productDto)
        productRepository.addVersion(name, pv).map(draft => productTimelineRepository.setDraftProduct(draft.name, draft.version, effectiveFrom))
    }
  }

  def deleteDraft(name: String): Imports.WriteResult =
    findDraftByName(name).map(draft => productRepository.delete(draft)).getOrElse {
      throw new IllegalArgumentException(s"No draft exists for product [$name]")
    }

  def getSummaries: List[ProductSummaryDto] =
    productTimelineRepository.findAll.map(timeline => findEffectiveProduct(timeline.productName) orElse findDraftByName(timeline.productName)).flatten.map(ProductSummaryDto(_))

  def getTags: List[String] =
    productRepository.findAllTags

  private def getCurrentRelatedProduct(name: String): Option[RelatedProductSummary] =
    findEffectiveProduct(name).map(cv => RelatedProductSummary(name, cv.title))

  private def findDraftByName(productName: String) = findDraftAndFromDate(productName).map(_._1)

  private def findDraftAndFromDate(productName: String) =
    for {
      timeline <- productTimelineRepository.findByProductName(productName)
      productVersion <- timeline.findDraftVersion()
      product <- productRepository.findByNameAndVersion(timeline.productName, productVersion.version)
    } yield (product, productVersion.from)

  private def findEffectiveProduct(productName: String, effectiveAt: DateTime = DateTime.now) = findEffectiveProductAndFromDate(productName, effectiveAt).map(_._1)

  private def findEffectiveProductAndFromDate(productName: String, effectiveAt: DateTime = DateTime.now): Option[(PersistedCatalogueProduct, DateTime)] =
    for {
      timeline <- productTimelineRepository.findByProductName(productName)
      productVersion <- timeline.findEffectiveVersion(effectiveAt)
      product <- productRepository.findByNameAndVersion(timeline.productName, productVersion.version)
    } yield (product, productVersion.from)

  private def verifyAndConvert(productDto: ProductDto, created: DateTime = DateTime.now): NewCatalogueProduct = {

    implicit val attributeFinder = attributeService.finder

    NewCatalogueProduct(
      name = productDto.name.getOrElse(throw new IllegalArgumentException("ProductDto does not have a name")),
      title = productDto.title,
      description = productDto.description,
      attributes = transformAndValidate(productDto.attributes),
      serviceAddOns = productDto.serviceAddOns,
      tags = productDto.tags,
      created = created,
      lastModified = productDto.lastModified,
      productBlockList = productDto.productBlockList)
  }

  private[service] def transformAndValidate(attributes: Map[String, CatalogueAttributeDto])
                                           (implicit attributeFinder: AttributeFinder): Map[String, CatalogueAttribute] = {

    attributes.map {
      case (attributeName, attribute) =>

        attributeFinder(attribute.facet, attributeName) map {
          definition => {

            val transformedAttr: CatalogueAttribute = definition.kind match {
              case "Decimal" if attribute.value.isInstanceOf[BigDecimal] => CatalogueBigDecimalProductAttribute(attribute.value.asInstanceOf[BigDecimal], attribute.label, attribute.facet, attribute.referenceText, attribute.referenceUrl, attribute.evidenceKinds, attribute.relatedProducts.map(_.name))
              case "Decimal" if attribute.value.isInstanceOf[BigInt] => CatalogueBigDecimalProductAttribute(BigDecimal(attribute.value.asInstanceOf[BigInt]), attribute.label, attribute.facet, attribute.referenceText, attribute.referenceUrl, attribute.evidenceKinds, attribute.relatedProducts.map(_.name))
              case "Boolean" if attribute.value.isInstanceOf[Boolean] => CatalogueBooleanProductAttribute(attribute.value.asInstanceOf[Boolean], attribute.label, attribute.facet, attribute.referenceText, attribute.referenceUrl, attribute.evidenceKinds, attribute.relatedProducts.map(_.name))
              case "Integer" if attribute.value.isInstanceOf[BigInt] => CatalogueIntegerProductAttribute(attribute.value.asInstanceOf[BigInt].toInt, attribute.label, attribute.facet, attribute.referenceText, attribute.referenceUrl, attribute.evidenceKinds, attribute.relatedProducts.map(_.name))
              case "Integer" if attribute.value.isInstanceOf[Integer] => CatalogueIntegerProductAttribute(attribute.value.asInstanceOf[Integer], attribute.label, attribute.facet, attribute.referenceText, attribute.referenceUrl, attribute.evidenceKinds, attribute.relatedProducts.map(_.name))
              case _ => throw new IllegalArgumentException(s"Invalid attribute [$attributeName], unrecognised value of type [${attribute.value.getClass}]")
            }

            if (!validateAttribute(definition, transformedAttr))
              throw new IllegalArgumentException(s"[$definition] [$transformedAttr] [$attribute] Invalid attribute [$attributeName]")
            else
              (attributeName, transformedAttr)
          }
        } getOrElse (throw new IllegalArgumentException(s"Unrecognised attribute [$attributeName]"))
    }
  }

  private[service] def validateAttribute(definition: CatalogueAttributeDefinition, attribute: CatalogueAttribute): Boolean =
    attribute.kind == definition.kind && attribute.evidenceKinds.map(_.reference).toSet.subsetOf(definition.evidenceKinds.toSet)

  private def asProductDto(product: PersistedCatalogueProduct, effectiveFrom: DateTime): ProductDto = {

    val allAttributes = attributeService.catalogueAttributes
    val withAttributeLabelsAndValidValues = addAttributeLabel(allAttributes.map(a => a.name -> a.label).toMap) _

    val withAttributeValidValues = addAttributeValidValues(allAttributes.map(a => a.name -> a.validValues).toMap) _

    def convertRelatedProducts(attribute: CatalogueAttribute): CatalogueAttributeDto = CatalogueAttributeDto(
      attribute.value,
      attribute.label,
      attribute.facet,
      attribute.referenceText,
      attribute.referenceUrl,
      attribute.evidenceKinds,
      attribute.relatedProducts.map(name => getCurrentRelatedProduct(name)).flatten.toList
    )
    val attributesWithLabel: Map[String, CatalogueAttributeDto] = product.attributes.map(withAttributeLabelsAndValidValues).map {
      case (label, attr) =>
        (label, convertRelatedProducts(attr))
    }.toMap

    ProductDto(
      name = Some(product.name), // TODO: Why is this optional??!?
      title = product.title,
      description = product.description,
      attributes = attributesWithLabel.map(withAttributeValidValues),
      serviceAddOns = product.serviceAddOns,
      tags = product.tags,
      version = Some(product.version),
      lastModified = product.created,
      effectiveFrom = effectiveFrom.toLocalDate,
      productBlockList = product.productBlockList
    )
  }

  private def addAttributeLabel(labels: Map[String, String])(attribute: (String, CatalogueAttribute)): (String, CatalogueAttribute) = {
    val attr = attribute._2 match {
      case a: CatalogueBigDecimalProductAttribute => a.copy(label = labels.getOrElse(attribute._1, attribute._1))
      case a: CatalogueIntegerProductAttribute => a.copy(label = labels.getOrElse(attribute._1, attribute._1))
      case a: CatalogueBooleanProductAttribute => a.copy(label = labels.getOrElse(attribute._1, attribute._1))
    }
    (attribute._1, attr)
  }

  private def addAttributeValidValues(nameToValidValues: Map[String, Map[String, Int]])(nameToAttrDto: (String, CatalogueAttributeDto)) = {
    val (name, attrbute) = nameToAttrDto
    (name, attrbute.copy(validValues = nameToValidValues.getOrElse(name, Map())))
  }
}
