package cjp.catalogue.resource

import java.util.Date

import cjp.catalogue.model._
import cjp.catalogue.service.{CatalogueAttributeDto, RelatedProductSummary}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.json4s.JsonAST.{JArray, JBool, JDecimal, JField, JInt, JObject, JString, JValue}
import org.json4s._
import org.json4s.ext.DateTimeSerializer

object CjpFormats {

  class LocalDateSerializer extends CustomSerializer[LocalDate](format => ( {
    case JString(s) => LocalDate.parse(s, DateTimeFormat.forPattern("yyyy-MM-dd"))
    case JArray(List(JInt(y), JInt(m), JInt(d))) => new LocalDate(y.toInt, m.toInt, d.toInt)
    case a => throw new IllegalArgumentException("xxxx" + a.toString)
  }, {
    case x: LocalDate => JString(x.toString("yyyy-MM-dd"))
  }
    ))

  def cjpFormats = new Formats {
    val dateFormat = DefaultFormats.lossless.dateFormat
    override val wantsBigDecimal: Boolean = true
  }

  implicit val formats = cjpFormats + DateTimeSerializer + new LocalDateSerializer + new ProductAttributeSerializer + new CatalogueAttributeSerializer

}
//TODO chage this so that its not dependent on the order of the fields in the JSON payload
class ProductAttributeSerializer extends CustomSerializer[CatalogueAttributeDto](implicit format => ( {
  case JObject(JField("value", JDecimal(v)) :: JField("label", JString(l)) :: JField("facet", JString(f)) :: JField("referenceText", JString(rt)) :: JField("referenceUrl", JString(ru)) :: JField("evidenceKinds", ek) :: JField("relatedProducts", rp) :: x) =>
    new CatalogueAttributeDto(v, label = "", facet = f, rt, ru, ek.extract[List[EvidenceKind]], rp.extract[List[RelatedProductSummary]], Map())
  case JObject(JField("value", JInt(v)) :: JField("label", JString(l)) :: JField("facet", JString(f)) :: JField("referenceText", JString(rt)) :: JField("referenceUrl", JString(ru)) :: JField("evidenceKinds", ek) :: JField("relatedProducts", rp) :: JField("validValues", av) :: x) if !av.extract[Map[String, Int]].isEmpty =>
    new CatalogueAttributeDto(v, label = "", facet = f, rt, ru, ek.extract[List[EvidenceKind]], rp.extract[List[RelatedProductSummary]], av.extract[Map[String, Int]])
  case JObject(JField("value", JInt(v)) :: JField("label", JString(l)) :: JField("facet", JString(f)) :: JField("referenceText", JString(rt)) :: JField("referenceUrl", JString(ru)) :: JField("evidenceKinds", ek) :: JField("relatedProducts", rp) :: x) =>
    new CatalogueAttributeDto(v, label = "", facet = f, rt, ru, ek.extract[List[EvidenceKind]], rp.extract[List[RelatedProductSummary]], Map())
  case JObject(JField("value", JBool(v)) :: JField("label", JString(l)) :: JField("facet", JString(f)) :: JField("referenceText", JString(rt)) :: JField("referenceUrl", JString(ru)) :: JField("evidenceKinds", ek) :: JField("relatedProducts", rp) :: x) =>
    new CatalogueAttributeDto(v, label = "", facet = f, rt, ru, ek.extract[List[EvidenceKind]], rp.extract[List[RelatedProductSummary]], Map())
}, {
  case attr: CatalogueAttributeDto => CatalogueSerialisationHelper.serializeCatalogueAttributeDto(attr)
}
  ))


class CatalogueAttributeSerializer extends {
  val jValue: (String, List[(String, JValue)]) => JValue =
    (n, ls) => {
      ls.toMap.getOrElse(n, throw new RuntimeException(s"can't find value for field : $n in the product attributes payload"))
    }
  val jString: (String, List[(String, JValue)]) => String = (n, ls) => {
    val JString(v) = jValue(n, ls)
    v
  }

} with CustomSerializer[CatalogueAttribute](implicit format => ( {
  case JObject(x) if jValue("value", x).isInstanceOf[JDecimal] =>
    val JDecimal(v) = jValue("value", x)
    new CatalogueBigDecimalProductAttribute(
      v,
      label = "",
      jString("facet", x),
      jString("referenceText", x),
      jString("referenceUrl", x),
      jValue("evidenceKinds", x).extract[List[EvidenceKind]],
      jValue("relatedProducts", x).extract[List[String]])
  case JObject(x) if jValue("value", x).isInstanceOf[JInt] =>
    val JInt(v) = jValue("value", x)
    new CatalogueIntegerProductAttribute(
      v.toInt,
      label = "",
      jString("facet", x),
      jString("referenceText", x),
      jString("referenceUrl", x),
      jValue("evidenceKinds", x).extract[List[EvidenceKind]],
      jValue("relatedProducts", x).extract[List[String]])
  case JObject(x) if jValue("value", x).isInstanceOf[JBool] =>
    val JBool(v) = jValue("value", x)
    new CatalogueBooleanProductAttribute(
      v,
      label = "",
      jString("facet", x),
      jString("referenceText", x),
      jString("referenceUrl", x),
      jValue("evidenceKinds", x).extract[List[EvidenceKind]],
      jValue("relatedProducts", x).extract[List[String]])
}, {
  case attr: CatalogueAttribute => CatalogueSerialisationHelper.serializeCatalogueAttribute(attr)
}
  ))

class DateSerializer extends CustomSerializer[Date](format => ( {
  case JInt(s) =>
    new Date(s.toLong)
  case a@_ =>
    throw new IllegalArgumentException("xxxx" + a)
}, {
  case x: Date => JInt(x.getTime)
}
  ))

object CatalogueSerialisationHelper {
  def serializeCatalogueAttributeDto(attr: CatalogueAttributeDto)(implicit format: Formats): JObject = {
    JObject(List(
      JField("value", serialiseValue(attr.value)),
      JField("label", JString(attr.label)),
      JField("facet", JString(attr.facet)),
      JField("referenceText", JString(attr.referenceText)),
      JField("referenceUrl", JString(attr.referenceUrl)),
      JField("evidenceKinds", Extraction.decompose(attr.evidenceKinds)),
      JField("relatedProducts", Extraction.decompose(attr.relatedProducts)),
      JField("validValues", Extraction.decompose(attr.validValues))
    )
    )
  }

  def serializeCatalogueAttribute(attr: CatalogueAttribute)(implicit format: Formats): JObject = {
    JObject(List(
      JField("value", serialiseValue(attr.value)),
      JField("label", JString(attr.label)),
      JField("facet", JString(attr.facet)),
      JField("referenceText", JString(attr.referenceText)),
      JField("referenceUrl", JString(attr.referenceUrl)),
      JField("evidenceKinds", Extraction.decompose(attr.evidenceKinds)),
      JField("relatedProducts", Extraction.decompose(attr.relatedProducts))
    )
    )
  }

  def serialiseValue(value: Any): JValue = value match {
    case i: Int => JInt(i)
    case b: Boolean => JBool(b)
    case d: BigDecimal => JDecimal(d)
  }
}
