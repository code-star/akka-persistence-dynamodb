/**
 * Copyright (C) 2016 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.persistence.dynamodb.query

import akka.Done
import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.{ ask, pipe }
import akka.persistence.dynamodb.journal._
import akka.persistence.dynamodb.{ DynamoDBRequests, dynamoClient }
import akka.serialization.SerializationExtension
import com.amazonaws.services.dynamodbv2.model.{ DeleteRequest, ScanRequest, WriteRequest }
import org.scalatest.BeforeAndAfter

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

trait Cleanup extends BeforeAndAfter { this: DynamoDBSpec ⇒

  before {
    log.debug(s"cleanup before test")
    val actor = system.actorOf(CleanupActor.props(settings))
    val deleted = (actor ? "delete").mapTo[Done]
    Await.result(deleted, 5.seconds)
  }

}

class CleanupActor(val settings: DynamoDBJournalConfig) extends Actor with DynamoDBRequests[DynamoDBJournalConfig] with ActorLogging {
  val dynamo = dynamoClient(context.system, settings)
  val serialization = SerializationExtension(context.system)
  import context.dispatcher
  import settings._

  override def receive = {
    case "delete" => delete.pipeTo(sender())
  }

  def delete: Future[Done] = {
    println(s"scanning $JournalTable")
    val scanRequest = new ScanRequest().withTableName(JournalTable)

    for {
      scanResult <- dynamo.scan(scanRequest)
      done <- doBatch(
        batch => s"execute batch delete $batch",
        {
          println(s"${scanResult.getItems.size()} items to delete")
          scanResult.getItems.asScala.map { item =>
            val partitionKey = item.get(Key).getS
            val sortKey = item.get(Sort).getN.toLong
            val key = item.asScala.filter { case (key, _) => key == Key || key == Sort }.asJava
            println(s"keys $key")
            new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(key))
          }
        }
      )

    } yield done

  }
}

object CleanupActor {
  def props(settings: DynamoDBJournalConfig): Props = Props(classOf[CleanupActor], settings)
}