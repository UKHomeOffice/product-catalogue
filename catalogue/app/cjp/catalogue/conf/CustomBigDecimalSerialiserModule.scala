package cjp.catalogue.conf

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.core.{JsonToken, JsonParseException, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext}
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import scala.util.{Failure, Success, Try}

class CustomBigDecimalDeserialiserModule extends SimpleModule("CustomBigDecimalDeserialiserModule") {
  addDeserializer(classOf[BigDecimal], BigDecimalDeserializer)
}

object BigDecimalDeserializer extends StdScalarDeserializer[BigDecimal](classOf[BigDecimal]) {

  override def deserialize(jsonParser: JsonParser, context: DeserializationContext): BigDecimal = {
    Option(jsonParser.getValueAsString) match {
      case Some(str: String) => Try(BigDecimal(str)) match {
        case Success(value) => value
        case Failure(error) => throw new JsonParseException(s"Unable to parse ${str} to BigDecimal", jsonParser.getCurrentLocation, error)
      }
      case None if jsonParser.getCurrentToken == JsonToken.VALUE_NULL => null.asInstanceOf[BigDecimal]
      case _ => throw new JsonParseException(s"Expected a string token representing type 'BigDecimal', found: ${jsonParser.getCurrentToken}", jsonParser.getCurrentLocation)
    }
  }
}


