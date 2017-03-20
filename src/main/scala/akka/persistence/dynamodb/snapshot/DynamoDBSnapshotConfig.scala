/**
 * Copyright (C) 2016 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.persistence.dynamodb.snapshot

import akka.persistence.dynamodb.journal.{ ClientConfig, DynamoDBClientConfig, DynamoDBConfig }
import com.typesafe.config.Config

class DynamoDBSnapshotConfig(c: Config) extends DynamoDBConfig {
  val SnapshotTable = c getString "snapshot-table"
  val SnapshotName = c getString "snapshot-name"
  val AwsKey = c getString "aws-access-key-id"
  val AwsSecret = c getString "aws-secret-access-key"
  val Endpoint = c getString "endpoint"

  override def toString: String = "DynamoDBJournalConfig(" +
    "SnapshotTable:" + SnapshotTable +
    ",AwsKey:" + AwsKey +
    ",Endpoint:" + Endpoint + ")"

  override val client: ClientConfig = new DynamoDBClientConfig(c)

  override val ClientDispatcher = c getString "client-dispatcher"
  override val Tracing = c getBoolean "tracing"

}
