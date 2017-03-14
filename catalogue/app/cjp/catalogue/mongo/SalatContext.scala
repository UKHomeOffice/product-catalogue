package cjp.catalogue.mongo

import com.novus.salat.Context

class SalatContext(val name: String) extends Context {
}

object SalatContext {

  implicit val defaultCtx = SalatContext("Default_Context")

  def apply(name: String): Context = {
    val ctx = new SalatContext(name)

    JodaTimeConverters.register(ctx)

    ctx
  }
}