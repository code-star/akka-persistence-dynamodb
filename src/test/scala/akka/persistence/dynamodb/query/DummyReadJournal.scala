/**
  * Copyright (C) 2016 Typesafe Inc. <http://www.typesafe.com>
  */
package akka.persistence.dynamodb.query

import akka.NotUsed
import akka.persistence.query.{ReadJournalProvider}
import akka.persistence.query.scaladsl.{AllPersistenceIdsQuery, ReadJournal}
import akka.stream.scaladsl.Source
import com.typesafe.config.{Config, ConfigFactory}
import akka.persistence.query.{javadsl => akkajavadsl}

/**
 * Use for tests only!
 * Emits infinite stream of strings (representing queried for events).
 */
class DummyReadJournal extends ReadJournal with AllPersistenceIdsQuery {
  override def allPersistenceIds(): Source[String, NotUsed] =
    Source.fromIterator(() ⇒ Iterator.from(0)).map(_.toString)
}

object DummyReadJournal {
  final val Identifier = "akka.persistence.query.journal.dummy"
}

class DummyReadJournalForJava(readJournal: DummyReadJournal) extends akkajavadsl.ReadJournal with akkajavadsl.AllPersistenceIdsQuery {
  override def allPersistenceIds(): akka.stream.javadsl.Source[String, NotUsed] =
    readJournal.allPersistenceIds().asJava
}

object DummyReadJournalProvider {
  final val config: Config = ConfigFactory.parseString(
    s"""
      |${DummyReadJournal.Identifier} {
      |  class = "${classOf[DummyReadJournalProvider].getCanonicalName}"
      |}
    """.stripMargin)
}

class DummyReadJournalProvider extends ReadJournalProvider {

  override val scaladslReadJournal: DummyReadJournal =
    new DummyReadJournal

  override val javadslReadJournal: DummyReadJournalForJava =
    new DummyReadJournalForJava(scaladslReadJournal)
}
