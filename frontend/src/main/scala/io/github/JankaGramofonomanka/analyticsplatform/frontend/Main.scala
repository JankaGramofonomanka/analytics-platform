package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.{ExitCode, IO, IOApp}

import org.apache.kafka.clients.producer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.DB
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.AerospikeClientIO
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Config
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Server
import io.github.JankaGramofonomanka.analyticsplatform.frontend.KafkaPublisher
import io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs.IOEntityCodec

object Main extends IOApp {
  def run(args: List[String]) = {

    implicit val env: Config.Environment = new Config.ActualEnvironment

    val aerospikeClient = Config.Common.Aerospike.getClient
    val clientIO = new AerospikeClientIO(aerospikeClient)
    val db = new DB(clientIO)
    
    val producer = new KafkaProducer[Nothing, UserTag](Config.Kafka.getProducerProps)
    
    val tagsToAggregate = new KafkaPublisher(producer, env.KAFKA_TOPIC)

    implicit val codec = IOEntityCodec
    Server.stream[IO](
      db.Profiles,
      db.Aggregates,
      tagsToAggregate,
      
    ).compile.drain.as(ExitCode.Success)
  }
}
