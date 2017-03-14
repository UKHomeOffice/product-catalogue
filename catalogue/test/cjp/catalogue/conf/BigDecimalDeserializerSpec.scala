package cjp.catalogue.conf

import org.scalatest.{Matchers, BeforeAndAfter, WordSpec}
import org.scalatest.mock.MockitoSugar.{ mock => smock }
import com.fasterxml.jackson.core.{JsonParseException, JsonParser}
import com.fasterxml.jackson.databind.DeserializationContext
import org.mockito.Mockito._


class BigDecimalDeserializerSpec extends WordSpec with Matchers with BeforeAndAfter {

  private val mockParser = smock[JsonParser]
  private val mockContext = smock[DeserializationContext]

  before {
    reset(mockContext, mockParser)
  }



  "deserialise" should {
    "correctly deserialise a decimal" in {
      when(mockParser.getValueAsString).thenReturn("132.32")

      BigDecimalDeserializer.deserialize(mockParser, mockContext) should equal(BigDecimal(132.32))

      verify(mockParser).getValueAsString
      verifyNoMoreInteractions(mockParser, mockContext)

    }

    "correctly deserialise an integer" in {
      when(mockParser.getValueAsString).thenReturn("111")

      BigDecimalDeserializer.deserialize(mockParser, mockContext) should equal(BigDecimal(111))

      verify(mockParser).getValueAsString
      verifyNoMoreInteractions(mockParser, mockContext)
    }

    "fail if its not a number" in {
      when(mockParser.getValueAsString).thenReturn("111.2.2")

      intercept[JsonParseException](BigDecimalDeserializer.deserialize(mockParser, mockContext))

      verify(mockParser).getValueAsString
    }
  }
}
