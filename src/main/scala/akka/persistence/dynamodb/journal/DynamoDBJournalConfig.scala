/**
 * Copyright (C) 2016 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.persistence.dynamodb.journal

import java.nio.ByteBuffer
import java.util.{HashMap => JHMap}

import akka.persistence.PersistentRepr
import com.typesafe.config.Config
import akka.persistence.dynamodb._
import akka.serialization.Serialization
import akka.util.ByteString

class DynamoDBJournalConfig(c: Config) extends DynamoDBConfig {
  val JournalTable = c getString "journal-table"
  val Table = JournalTable
  val JournalName = c getString "journal-name"
  val AwsKey = c getString "aws-access-key-id"
  val AwsSecret = c getString "aws-secret-access-key"
  val Endpoint = c getString "endpoint"
  val ReplayDispatcher = c getString "replay-dispatcher"
  val ClientDispatcher = c getString "client-dispatcher"
  val SequenceShards = c getInt "sequence-shards"
  val ReplayParallelism = c getInt "replay-parallelism"
  val Tracing = c getBoolean "tracing"
  val LogConfig = c getBoolean "log-config"

  val MaxBatchGet = c getInt "aws-api-limits.max-batch-get"
  val MaxBatchWrite = c getInt "aws-api-limits.max-batch-write"
  val MaxItemSize = c getInt "aws-api-limits.max-item-size"

  val client = new DynamoDBClientConfig(c)
  override def toString: String = "DynamoDBJournalConfig(" +
    "JournalTable:" + JournalTable +
    ",JournalName:" + JournalName +
    ",AwsKey:" + AwsKey +
    ",Endpoint:" + Endpoint +
    ",ReplayDispatcher:" + ReplayDispatcher +
    ",ClientDispatcher:" + ClientDispatcher +
    ",SequenceShards:" + SequenceShards +
    ",ReplayParallelism" + ReplayParallelism +
    ",Tracing:" + Tracing +
    ",MaxBatchGet:" + MaxBatchGet +
    ",MaxBatchWrite:" + MaxBatchWrite +
    ",MaxItemSize:" + MaxItemSize +
    ",client.config:" + client

  def messageKey(persistenceId: String, sequenceNr: Long): Item = {
    val item: Item = new JHMap
    item.put(Key, S(messagePartitionKey(persistenceId, sequenceNr)))
    item.put(Sort, N(sequenceNr % 100))
    item
  }

  def messagePartitionKey(persistenceId: String, sequenceNr: Long): String =
    s"$JournalName-P-$persistenceId-${sequenceNr / 100}"

  def highSeqKey(persistenceId: String, shard: Long) = {
    val item: Item = new JHMap
    item.put(Key, S(s"$JournalName-SH-$persistenceId-$shard"))
    item.put(Sort, Naught)
    item
  }

  def lowSeqKey(persistenceId: String, shard: Long) = {
    val item: Item = new JHMap
    item.put(Key, S(s"$JournalName-SL-$persistenceId-$shard"))
    item.put(Sort, Naught)
    item
  }

  def persistentToByteBuffer(p: PersistentRepr)(implicit serialization:Serialization): ByteBuffer =
    ByteBuffer.wrap(serialization.serialize(p).get)

  def persistentFromByteBuffer(b: ByteBuffer)(implicit serialization:Serialization): PersistentRepr = {
    serialization.deserialize(ByteString(b).toArray, classOf[PersistentRepr]).get
  }
}
