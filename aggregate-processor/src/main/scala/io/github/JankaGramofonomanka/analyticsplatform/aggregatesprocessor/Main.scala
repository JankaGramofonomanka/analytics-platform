package io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor

import cats.effect.{ExitCode, IO, IOApp}

import java.util.Collections

import org.apache.kafka.clients.consumer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Aerospike
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.Server
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.Config
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.KafkaSubscriber



object Main extends IOApp {

  def run(args: List[String]) = {

    implicit val env: Config.Environment = new Config.ActualEnvironment
    
    val db = new Aerospike.DB(Config.Common.Aerospike.getClient)

    val consumer = new KafkaConsumer[Nothing, UserTag](Config.Kafka.getConsumerProps)
    consumer.subscribe(Collections.singletonList(env.KAFKA_TOPIC))

    val tagsToAggregate = new KafkaSubscriber(consumer)
    
    Server.stream[IO](db.Aggregates, tagsToAggregate).compile.drain.as(ExitCode.Success)
  }
}


