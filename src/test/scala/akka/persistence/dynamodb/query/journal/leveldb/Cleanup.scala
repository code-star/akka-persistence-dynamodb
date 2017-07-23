/**
  * Copyright (C) 2016 Typesafe Inc. <http://www.typesafe.com>
  */
package akka.persistence.dynamodb.query.journal.leveldb

import java.io.File

import akka.persistence.dynamodb.query.DynamoDBSpec
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}

trait Cleanup extends BeforeAndAfter{ this: DynamoDBSpec ⇒
  val storageLocations = List(
    "akka.persistence.journal.leveldb.dir",
    "akka.persistence.journal.leveldb-shared.store.dir",
    "akka.persistence.snapshot-store.local.dir").map(s ⇒ new File(system.settings.config.getString(s)))

  before {
    //client.query()
  }


}
