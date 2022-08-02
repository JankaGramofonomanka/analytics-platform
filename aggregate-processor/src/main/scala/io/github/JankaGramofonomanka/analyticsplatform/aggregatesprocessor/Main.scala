package io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor

import cats.effect.{ExitCode, IO, IOApp}

import java.util.Collections

import org.apache.kafka.clients.consumer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.AggregateProcessorServer
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.KafkaTopic
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Aerospike


object Main extends IOApp {

  def run(args: List[String]) = {
    

    val aggregates = new Aerospike.DB(Config.Aerospike.client, Config.Aerospike.config)

    val consumer = new KafkaConsumer[String, UserTag](Config.Kafka.getConsumerProps)

    consumer.subscribe(Collections.singletonList(Config.Kafka.TOPIC))


    val tagsToAggregate = new KafkaTopic.Subscriber(consumer)
    
    AggregateProcessorServer.stream[IO](tagsToAggregate, aggregates).compile.drain.as(ExitCode.Success)
  }
  
}


