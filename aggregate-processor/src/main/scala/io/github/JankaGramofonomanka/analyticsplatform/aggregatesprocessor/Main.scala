package io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor

import cats.effect.{ExitCode, IO, IOApp}

import java.util

import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.serialization.StringDeserializer

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.AggregateProcessorServer
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.KafkaTopic
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.Kafka.UserTagDeserializer


object AggregateProcessor extends IOApp {

  def run(args: List[String]) = {
    import java.util.Properties

    val TOPIC = "test"

    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   classOf[StringDeserializer])
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[UserTagDeserializer])
    props.put("group.id", "something")

    val consumer = new KafkaConsumer[String, UserTag](props)

    consumer.subscribe(util.Collections.singletonList(TOPIC))

    val tagsToAggregate = new KafkaTopic.Subscriber(consumer)
    AggregateProcessorServer.stream[IO](tagsToAggregate).compile.drain.as(ExitCode.Success)
  }
  
}


