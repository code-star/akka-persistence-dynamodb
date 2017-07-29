package akka.persistence.dynamodb.query.scaladsl

import akka.actor.ExtendedActorSystem
import com.typesafe.config.Config
import java.net.URLEncoder

import akka.NotUsed
import akka.actor.ExtendedActorSystem
import akka.event.Logging
import akka.persistence.query.{EventEnvelope, EventEnvelope2, Offset}
import akka.stream.javadsl
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import akka.persistence.query.scaladsl._

class DynamoDBReadJournal(system: ExtendedActorSystem, config: Config) extends ReadJournal
    with AllPersistenceIdsQuery with CurrentPersistenceIdsQuery
    with EventsByPersistenceIdQuery with CurrentEventsByPersistenceIdQuery
    with EventsByTagQuery2 with CurrentEventsByTagQuery2 {
  override def allPersistenceIds(): Source[String, NotUsed] = ???

  override def currentPersistenceIds(): Source[String, NotUsed] = ???

  override def eventsByPersistenceId(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long): Source[EventEnvelope, NotUsed] = ???

  override def currentEventsByPersistenceId(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long): Source[EventEnvelope, NotUsed] = ???

  override def eventsByTag(tag: String, offset: Offset): Source[EventEnvelope2, NotUsed] = ???

  override def currentEventsByTag(tag: String, offset: Offset): Source[EventEnvelope2, NotUsed] = ???
}

object DynamoDBReadJournal {
  val Identifier = "dynamodb.query"
}
