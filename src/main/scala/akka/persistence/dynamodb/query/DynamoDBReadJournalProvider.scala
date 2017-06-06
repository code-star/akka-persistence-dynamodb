package akka.persistence.dynamodb.query
import akka.actor.ExtendedActorSystem
import akka.persistence.query.ReadJournalProvider
import com.typesafe.config.Config

class DynamoDBReadJournalProvider(system: ExtendedActorSystem, config: Config) extends ReadJournalProvider {

  override val scaladslReadJournal: scaladsl.DynamoDBReadJournal =
    new scaladsl.DynamoDBReadJournal(system, config)

  override val javadslReadJournal: javadsl.DynamoDBReadJournal =
    new javadsl.DynamoDBReadJournal(scaladslReadJournal)

}