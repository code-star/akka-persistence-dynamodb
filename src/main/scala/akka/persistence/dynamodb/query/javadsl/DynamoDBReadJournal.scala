package akka.persistence.dynamodb.query.javadsl
import akka.persistence.dynamodb.query.scaladsl.{ DynamoDBReadJournal => ScalaReadJournal }
import akka.persistence.query.javadsl._
import akka.NotUsed
import akka.persistence.query.EventEnvelope
import akka.stream.javadsl.Source

class DynamoDBReadJournal(scalaReadJournal: ScalaReadJournal) extends ReadJournal
    with AllPersistenceIdsQuery with CurrentPersistenceIdsQuery
    with EventsByPersistenceIdQuery with CurrentEventsByPersistenceIdQuery
    with EventsByTagQuery with CurrentEventsByTagQuery {
  override def allPersistenceIds(): Source[String, NotUsed] = ???

  override def currentPersistenceIds(): Source[String, NotUsed] = ???

  override def eventsByPersistenceId(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long): Source[EventEnvelope, NotUsed] = ???

  override def currentEventsByPersistenceId(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long): Source[EventEnvelope, NotUsed] = ???

  override def eventsByTag(tag: String, offset: Long): Source[EventEnvelope, NotUsed] = ???

  override def currentEventsByTag(tag: String, offset: Long): Source[EventEnvelope, NotUsed] = ???
}
