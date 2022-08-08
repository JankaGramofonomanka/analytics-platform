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
  

  class Publisher(producer: KafkaProducer[Nothing, UserTag], topicName: String)
  extends Topic.Publisher[IO, UserTag] {

    def publish(tag: UserTag): IO[Unit] = IO.delay {

      val record = new ProducerRecord(topicName, tag)
      producer.send(record).get
      ()
    }
  }

  class Subscriber(consumer: KafkaConsumer[Nothing, UserTag])
  extends Topic.Subscriber[IO, UserTag] {
    
    def subscribe: Stream[IO, UserTag] = Stream.evalSeq {
    
      IO.delay {
        val records = consumer.poll(Duration.ofMillis(Config.Kafka.pollTimeoutMillis))
        records.asScala.map(_.value).toSeq
      }

    }.repeat
  }
}
