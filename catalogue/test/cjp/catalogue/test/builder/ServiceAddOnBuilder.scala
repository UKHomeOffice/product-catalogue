package cjp.catalogue.test.builder

import cjp.catalogue.model.ServiceAddOn

object ServiceAddOnBuilder {
  def apply(name: String = "someName",
            title: String = "some title",
            description: String  = "someDescription",
            cost: Double = 250.5d,
            duration: Integer = 5) =
    ServiceAddOn(
      name = name,
      title = title,
      description = description,
      cost = cost,
      duration = duration
    )
}