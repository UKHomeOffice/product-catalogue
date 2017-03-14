package cjp.catalogue.mongo

import com.novus.salat.Context
import com.novus.salat.transformers.CustomTransformer
import org.bson.{BSON, Transformer}
import org.joda.time.{DateTime, LocalDateTime}

/** Casbah's Joda LocalDateTime converter is deeply broken. */
object JodaTimeConverters {
  private val dateTimeClass = classOf[DateTime]
  private val localDateTimeClass = classOf[LocalDateTime]

  private val bsonEncoder = new Transformer {
    def transform(o: AnyRef): AnyRef = o match {
      case d: DateTime => d.toDate
      case l: LocalDateTime => l.toString
      case _ => o
    }
  }

  private val localDateTimeTransformer = new CustomTransformer[LocalDateTime, Object]() {
    override def serialize(a: LocalDateTime): String = a.toString

    override def deserialize(b: Object): LocalDateTime = b match {
      case s: String => new LocalDateTime(s)
      case d: java.util.Date => new LocalDateTime(d) // For backward compatibility only - not a good idea
    }
  }

  private val dateTimeTransformer = new CustomTransformer[DateTime, java.util.Date]() {
    override def serialize(a: DateTime): java.util.Date = a.toDate
    override def deserialize(b: java.util.Date): DateTime = new DateTime(b)
  }

  def register(ctx: Context): Unit = {
    BSON.addEncodingHook(dateTimeClass, bsonEncoder)
    BSON.addEncodingHook(localDateTimeClass, bsonEncoder)
    ctx.registerCustomTransformer(dateTimeTransformer)
    ctx.registerCustomTransformer(localDateTimeTransformer)
  }
}
