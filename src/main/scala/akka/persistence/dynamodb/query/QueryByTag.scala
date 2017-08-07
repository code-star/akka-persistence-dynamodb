package akka.persistence.dynamodb.query

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.dynamodb.journal.DynamoDBJournalConfig
import akka.stream.scaladsl.Source
import java.util.{ Collections, HashMap => JHMap, List => JList, Map => JMap }

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import akka.persistence.dynamodb._
import com.amazonaws.services.dynamodbv2.model._

import scala.collection.immutable

object QueryByTag {
}

class QueryByTag(val settings: DynamoDBJournalConfig)(implicit actorSystem: ActorSystem) {
  implicit val dynamo = dynamoClient(actorSystem, settings)

  import actorSystem.dispatcher
  import settings._

  val log = dynamo.log

  //we'll have a new table called 'tags'. There we keep track of which persistenceids there are for a tag.
  // But not the individual events. They are only stored in the journal table. The reason for that is that any
  // query optimized table, like a 'eventsForTag', would be throttled heavily and undo the optimisation that
  // the journal implements to facilitate high throughput bursts
  object Tag {
    val Table = "tags" //TODO get from config
    val Par = "tag" // tag value, the partition key
    val Sort = "pid" // persistence id, the sort key
    val Seq = "seq" // the highest seq nr for the pid, multiple of 100. Ie /100. This will be the same as the highest value of HS in the journal for this persistence id.
    val SeqIndex = "seq-idx" // the name of the secondary index that sorts by seq value
  }

  val JournalTimestamp = "ts" // we need to add a timestamp value to each journal item

  /* execution plan for query 'currentEventsByTag'
  *
  * query 'tags' for $tag, read pid-seq/100 combinations. paginated, sorted by seq. we need a secondary index on 'tags' for this.
  *  for each pid-seq/100:
  *    query for pid-seq/100, paginated results. (these calls can't be batched)
  *  concat results so that each for a pid the results are in the correct order
  *
  * to facilitate this the writes need to be adapted:
  * on every new persistenceid, and every hundredth (seq/100), put or update the 'tags' table.
  */

  pidsByTag(tag = "person")
    .flatMapConcat { query =>
      val state: Option[Key] = None
      Source.unfoldAsync(state)(queryEvents(query))
    }

  def pidsByTag(tag: String)(implicit executionContext: ExecutionContext): Source[TagEntry, NotUsed] = {
    val start: Option[Key] = None
    Source.unfoldAsync(start)(queryAll(tag)) //for a pid the results will be sorted by seq nr
      .flatMapConcat { items =>
        Source[TagEntry](items.map(item => TagEntry.fromDynamodb(item)).to[immutable.Seq])
      }
  }

  type Key = Item

  def queryEvents(tagEntry: TagEntry): Option[Key] => Future[Option[(Option[Key], Seq[Item])]] = {
    case None => //initial
      log.debug("initial")
      queryForEvent(tagEntry, lastKey = null, msg = "initial")

    case Some(null) => //scalastyle:ignore null
      //no more
      log.debug("no more")
      Future.successful(None)

    case Some(lastkey) => //more
      log.debug("more")
      queryForEvent(tagEntry, lastKey = lastkey, msg = "more")

  }

  private def queryForEvent(tagEntry: TagEntry, lastKey: Key, msg: String) = {
    dynamo.query(eventQuery(tagEntry, lastKey))
      .map { result =>
        val nextState = Some(result.getLastEvaluatedKey)
        log.debug(s"received $msg")
        Some((nextState, result.getItems.asScala))
      }
      .recoverWith {
        case NonFatal(t) =>
          log.error("$msg failed", t)
          Future.failed(t)
      }
  }

  def queryAll(tag: String): Option[Key] => Future[Option[(Option[Key], Seq[Item])]] = {

    case None => //initial
      log.debug("initial")
      queryForTag(tag, lastKey = null, msg = "initial")

    case Some(null) => //scalastyle:ignore null
      //no more
      log.debug("no more")
      Future.successful(None)

    case Some(lastkey) => //more
      log.debug("more")
      queryForTag(tag, lastKey = lastkey, msg = "more")
  }

  private def queryForTag(tag: String, lastKey: Key, msg: String) = {
    dynamo.query(tagQuery(tag, lastKey))
      .map { result =>
        val nextState = Some(result.getLastEvaluatedKey)
        log.debug(s"received $msg")
        Some((nextState, result.getItems.asScala))
      }
      .recoverWith {
        case NonFatal(t) =>
          log.error("$msg failed", t)
          Future.failed(t)
      }
  }

  def tagQuery(tag: String, fromKey: Key) =
    new QueryRequest()
      .withTableName(Tag.Table)
      .withKeyConditionExpression(Tag.Par + " = :kkey")
      .withExpressionAttributeValues(Collections.singletonMap(":kkey", S(tag)))
      .withProjectionExpression(Set(Tag.Par, Tag.Seq, Tag.Sort).mkString(","))
      .withConsistentRead(true)
      .withExclusiveStartKey(fromKey)

  def eventQuery(tagEntry: TagEntry, fromKey: Key) = {
    new QueryRequest()
      .withTableName(JournalTable)
      .withKeyConditionExpression(journal.Key + " = :kkey")
      .withExpressionAttributeValues(Collections.singletonMap(":kkey", S(tagEntry.toPersistenceKey)))
      .withProjectionExpression(Set(journal.Key, journal.Sort, journal.Payload).mkString(","))
      .withConsistentRead(true)
      .withExclusiveStartKey(fromKey)

  }

  def batchGetReq(items: JMap[String, KeysAndAttributes]) =
    new BatchGetItemRequest()
      .withRequestItems(items)
      .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)

  case class TagEntry(tag: String, pid: String, seq: Long) {
    def toPersistenceKey = s"$JournalName-P-$pid-$seq"
  }

  object TagEntry {
    def fromDynamodb(item: Item): TagEntry = {
      val par = item.get(Tag.Par).getS
      val pid = item.get(Tag.Sort).getS
      val seq = item.get(Tag.Seq).getN.toLong
      TagEntry(par, pid, seq)
    }
  }
}

