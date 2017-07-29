package akka.persistence.dynamodb.query.javadsl
import akka.persistence.dynamodb.query.scaladsl.{DynamoDBReadJournal => ScalaReadJournal}
import akka.persistence.query.javadsl._
import akka.NotUsed
import akka.persistence.query.{EventEnvelope, EventEnvelope2, Offset}
import akka.stream.javadsl.Source

class DynamoDBReadJournal(scalaReadJournal: ScalaReadJournal) extends ReadJournal
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
