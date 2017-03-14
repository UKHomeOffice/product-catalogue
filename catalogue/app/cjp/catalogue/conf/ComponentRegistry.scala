import cjp.catalogue.mongo.MongoConnector
import cjp.catalogue.mongo.SalatContext._
import cjp.catalogue.repository._
import cjp.catalogue.resource._
import cjp.catalogue.service._
import cjp.catalogue.util.BuildInfo
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTimeUtils
import play.api._
import play.api.mvc.Controller


object Configuration {
  val conf: Config = ConfigFactory.load()

  val dbHosts = conf.getString("mongo.catalogue.hosts")
  val dbName = conf.getString("mongo.catalogue.db")
  val dbOptions = conf.getString("mongo.catalogue.options")
  val dbUrl = s"mongodb://$dbHosts/$dbName?$dbOptions"

  val enableTimemachine = conf.getBoolean("enableTimemachine")

}

object ComponentRegistry {
  defaultCtx.registerClassLoader(Play.classloader(Play.current))
  defaultCtx.registerClassLoader(Play.classloader(Play.current))

  private val mongo = new MongoConnector(Configuration.dbUrl)

  private val productRepository = new ProductRepository(mongo)
  private val productTimelineRepository = new ProductTimelineRepository(mongo)
  private val attributeRepository = new AttributeRepository(mongo)
  private val effectiveDateRepository = new EffectiveDateRepository(mongo)

  private val attributeService = new AttributeService(attributeRepository)
  private val productService = new ProductManagementService(productRepository, productTimelineRepository, attributeService)
  private val exportImportService = new ExportImportService(productRepository, productTimelineRepository, attributeService)
  private val addOnsService = new AddOnService(productRepository)

  if (Configuration.enableTimemachine) {
    DateTimeUtils.setCurrentMillisProvider(new TimeMachineMillisProvider(effectiveDateRepository))
  }

  private val controllers: Map[Class[_], Controller] = Seq(
    new ProductResource(productService, attributeService),
    new ManageProductResource(productService),
    new AttributeResource(attributeService),
    new EffectiveDateResource(effectiveDateRepository, Configuration.enableTimemachine),
    new ExportImportResource(exportImportService, new CatalogueImportValidator),
    new HealthCheckResource(new MongoHealthCheck(mongo), BuildInfo.fromApplicationConfig),
    new AddOnsResource(addOnsService)
  ).map { c =>
    c.getClass -> c
  }.toMap[Class[_], Controller]

  def getController[A](controllerClass: Class[A]): A = controllers(controllerClass).asInstanceOf[A]
}
