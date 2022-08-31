package io.github.JankaGramofonomanka.analyticsplatform.tagprocessor

import cats.effect.{ExitCode, IO, IOApp}

import java.util.Collections

import org.apache.kafka.clients.consumer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.DB
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.AerospikeClientIO
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.Server
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.Config
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.KafkaSubscriber



object Main extends IOApp {

  def run(args: List[String]) = {

    implicit val env: Config.Environment = new Config.ActualEnvironment
    
    val aerospikeClient = Config.Common.Aerospike.getClient
    val clientIO = new AerospikeClientIO(aerospikeClient)
    val db = new DB(clientIO)

    val consumer = new KafkaConsumer[Nothing, UserTag](Config.Kafka.getConsumerProps)
    consumer.subscribe(Collections.singletonList(env.KAFKA_TOPIC))

    val tagsToAggregate = new KafkaSubscriber(consumer)
    
    Server.stream[IO](db.Profiles, db.Aggregates, tagsToAggregate).compile.drain.as(ExitCode.Success)
  }
}


