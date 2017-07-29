package akka.persistence.dynamodb.query

import akka.actor.ActorSystem
import akka.dispatch.Dispatchers
import akka.event.{Logging, LoggingAdapter}
import akka.persistence.dynamodb.journal.DynamoDBUtils
import akka.testkit.TestEvent.Mute
import akka.testkit.{DeadLettersFilter, TestKit}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalactic.{ConversionCheckedTripleEquals, TypeCheckedTripleEquals}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/** inspired by AkkaSpec */
abstract class DynamoDBSpec(_system: ActorSystem)
    extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll
    with TypeCheckedTripleEquals with ScalaFutures with DynamoDBUtils {

  implicit val patience = PatienceConfig(testKitSettings.DefaultTimeout.duration, Span(100, org.scalatest.time.Millis))

  def this(config: Config) = this(ActorSystem(
    DynamoDBSpec.getCallerName(getClass),
    ConfigFactory.load(config.withFallback(DynamoDBSpec.testConf))
  ))

  def this(s: String) = this(ConfigFactory.parseString(s))

  def this(configMap: Map[String, _]) = this(DynamoDBSpec.mapToConfig(configMap))

  def this() = this(ActorSystem(DynamoDBSpec.getCallerName(getClass), DynamoDBSpec.testConf))

  val log: LoggingAdapter = Logging(system, this.getClass)

  override def beforeAll(): Unit = {
    super.beforeAll()
    ensureJournalTableExists()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    client.shutdown()
  }
}

object DynamoDBSpec {
  val testConf: Config = ConfigFactory.parseString("""
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
        stdout-loglevel = "WARNING"
        actor {
          default-dispatcher {
            executor = "fork-join-executor"
            fork-join-executor {
              parallelism-min = 8
              parallelism-factor = 2.0
              parallelism-max = 8
            }
          }
        }
      }
                                                    """)

  def mapToConfig(map: Map[String, Any]): Config = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseMap(map.asJava)
  }

  def getCallerName(clazz: Class[_]): String = {
    val s = (Thread.currentThread.getStackTrace map (_.getClassName) drop 1)
      .dropWhile(_ matches "(java.lang.Thread|.*AkkaSpec.?$|.*StreamSpec.?$)")
    val reduced = s.lastIndexWhere(_ == clazz.getName) match {
      case -1 ⇒ s
      case z  ⇒ s drop (z + 1)
    }
    reduced.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }

}
