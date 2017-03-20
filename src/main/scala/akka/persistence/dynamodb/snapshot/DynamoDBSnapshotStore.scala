package akka.persistence.dynamodb.snapshot

import java.util.HashMap

import akka.actor.ActorLogging
import akka.persistence.dynamodb.journal._
import akka.persistence.{ PersistentRepr, SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria }
import akka.persistence.snapshot.SnapshotStore
import akka.serialization.SerializationExtension
import com.amazonaws.services.dynamodbv2.model._
import com.typesafe.config.Config

import scala.concurrent.Future
import java.util.{ HashMap => JHMap, Map => JMap }

class DynamoDBSnapshotStore(config: Config) extends SnapshotStore with ActorLogging {
  val settings = new DynamoDBSnapshotConfig(config)
  val dynamo = dynamoClient(context.system, settings)
  val serialization = SerializationExtension(context.system)

  import settings._
  implicit val ec = akka.dispatch.ExecutionContexts.sameThreadExecutionContext

  override def preStart(): Unit = {
    // eager initialization, but not from constructor
    self ! DynamoDBSnapshotStore.Init
  }

  override def loadAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Option[SelectedSnapshot]] = {
    val gi = new GetItemRequest().withTableName(SnapshotTable).withKey(messageKey(persistenceId, criteria.maxSequenceNr))
    dynamo.getItem(gi).map(x => {
      //println(s"Found this: ${x.getItem} for key ${criteria.maxSequenceNr} in ${persistenceId}")
      Option.apply(x.getItem).map(map => {
        val attr = map.get(Payload)
        val time = map.get(Timestamp)
        SelectedSnapshot.create(SnapshotMetadata(persistenceId, criteria.maxSequenceNr, time.getS.toLong), attr.getS)
      })
    })
  }

  override def saveAsync(metadata: SnapshotMetadata, snapshot: Any): Future[Unit] = {
    //println(s"Save $snapshot for key ${metadata.sequenceNr} in ${metadata.persistenceId} on ${metadata.timestamp}")
    val item = messageKey(metadata.persistenceId, metadata.sequenceNr)
    item.put(Payload, new AttributeValue(snapshot.toString))
    item.put(Timestamp, new AttributeValue(metadata.timestamp.toString))
    //item.put(SequenceNr)
    val pi = new PutItemRequest().withTableName(SnapshotTable).withItem(item)
    dynamo.putItem(pi).map(_ => Unit)
  }

  override def deleteAsync(metadata: SnapshotMetadata): Future[Unit] = {
    val dl = new DeleteItemRequest().withTableName(SnapshotTable).withKey(messageKey(metadata.persistenceId, metadata.sequenceNr))
    dynamo.deleteItem(dl).map(_ => Unit)
  }

  override def deleteAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] = Future.successful(())

  def messageKey(persistenceId: String, sequenceNr: Long): Item = {
    val item: Item = new JHMap
    item.put(Key, S(messagePartitionKey(persistenceId, sequenceNr)))
    item.put(Sort, N(sequenceNr % 100))
    item
  }

  private def messagePartitionKey(persistenceId: String, sequenceNr: Long): String =
    s"$SnapshotName-P-$persistenceId-${sequenceNr / 100}"
}

object DynamoDBSnapshotStore {
  private case object Init
}