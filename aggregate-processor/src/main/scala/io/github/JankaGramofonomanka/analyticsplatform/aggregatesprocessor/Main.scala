package io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor

import cats.effect.{ExitCode, IO, IOApp}

import java.util.Collections

import org.apache.kafka.clients.consumer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.AggregateProcessorServer
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.Environment
import io.github.JankaGramofonomanka.analyticsplatform.common.ActualEnvironment
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.KafkaTopic
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.Aerospike


object Main extends IOApp {

  def run(args: List[String]) = {

    implicit val env: Environment = ActualEnvironment
    
    val db = new Aerospike.DB(Config.Aerospike.getClient, Config.Aerospike.getConfig)

    val consumer = new KafkaConsumer[Nothing, UserTag](Config.Kafka.getConsumerProps)
    consumer.subscribe(Collections.singletonList(env.KAFKA_TOPIC))

    val tagsToAggregate = new KafkaTopic.Subscriber(consumer)
    
    AggregateProcessorServer.stream[IO](db.Aggregates, tagsToAggregate).compile.drain.as(ExitCode.Success)
  }
}


