/**
 * Copyright (C) 2016 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.persistence.dynamodb.query.journal.leveldb

import akka.persistence.dynamodb.query.DynamoDBSpec
import akka.persistence.dynamodb.query.scaladsl.DynamoDBReadJournal

import scala.concurrent.duration._
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.AllPersistenceIdsQuery
import akka.stream.ActorMaterializer
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory

object AllPersistenceIdsSpec {
  val config = """
    akka.loglevel = INFO
    akka.persistence.journal.plugin = "akka.persistence.dynamodb.journal.DynamoDBJournal"
    akka.persistence.journal.leveldb.dir = "target/journal-AllPersistenceIdsSpec"
    akka.test.single-expect-default = 10s
    """
}

class AllPersistenceIdsSpec extends DynamoDBSpec(ConfigFactory.load())
    with Cleanup with ImplicitSender {

  implicit val mat = ActorMaterializer()(system)

  val queries = PersistenceQuery(system).readJournalFor[DynamoDBReadJournal](DynamoDBReadJournal.Identifier)

  "Dynomodb query AllPersistenceIds" must {

    "implement standard AllPersistenceIdsQuery" in {
      queries.isInstanceOf[AllPersistenceIdsQuery] should ===(true)
    }

    "find existing persistenceIds" in {
      system.actorOf(TestActor.props("a")) ! "a1"
      expectMsg("a1-done")
      system.actorOf(TestActor.props("b")) ! "b1"
      expectMsg("b1-done")
      system.actorOf(TestActor.props("c")) ! "c1"
      expectMsg("c1-done")

      val src = queries.currentPersistenceIds()
      val probe = src.runWith(TestSink.probe[String])
      probe.within(10.seconds) {
        probe.request(5)
          .expectNextUnordered("a", "b", "c")
          .expectComplete()
      }
    }

    "find new persistenceIds" in {
      // a, b, c created by previous step
      system.actorOf(TestActor.props("d")) ! "d1"
      expectMsg("d1-done")

      val src = queries.allPersistenceIds()
      val probe = src.runWith(TestSink.probe[String])
      probe.within(10.seconds) {
        probe.request(5)
          .expectNextUnorderedN(List("a", "b", "c", "d"))

        system.actorOf(TestActor.props("e")) ! "e1"
        probe.expectNext("e")

        val more = (1 to 100).map("f" + _)
        more.foreach { p ⇒
          system.actorOf(TestActor.props(p)) ! p
        }

        probe.request(100)
        probe.expectNextUnorderedN(more)
      }

    }
  }

}
