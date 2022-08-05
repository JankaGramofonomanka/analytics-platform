package io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic

import cats.effect.IO
import fs2.Stream

import org.apache.kafka.clients.consumer._
import org.apache.kafka.clients.producer._

import java.time.Duration
import scala.jdk.CollectionConverters._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic

object KafkaTopic {
  

  class Publisher(producer: KafkaProducer[String, UserTag], topicName: String)
  extends Topic.Publisher[IO, UserTag] {

    def publish(tag: UserTag): IO[Unit] = IO.delay {

      val record = new ProducerRecord(topicName, Config.Kafka.key, tag)

      // TODO !!!!!!!!!!! this is a future!!!!!
      // TODO what does the result value do?
      producer.send(record)
      ()
    }
  }

  class Subscriber(consumer: KafkaConsumer[String, UserTag])
  extends Topic.Subscriber[IO, UserTag] {
    
    def subscribe: Stream[IO, UserTag] = Stream.evalSeq {
    
      IO.delay {
        val records = consumer.poll(Duration.ofMillis(Config.Kafka.pollTimeoutMillis))
        records.asScala.map(_.value).toSeq
      }

    }.repeat
  }
}
