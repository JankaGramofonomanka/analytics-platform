package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats.effect.IO
import fs2.Stream

import org.apache.kafka.clients.consumer._
import org.apache.kafka.clients.producer._

import java.time.Duration
import scala.jdk.CollectionConverters._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic

object KafkaTopic {
  

  class Publisher(producer: KafkaProducer[String, UserTag], topicName: String)
  extends Topic.Publisher[IO, UserTag] {

    def publish(tag: UserTag): IO[Unit] = IO.delay {

      // TODO what is key?
      val key = "key"
      val record = new ProducerRecord(topicName, key, tag)

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
        // TODO move literal somewhere
        val records = consumer.poll(Duration.ofMillis(100))
        records.asScala.map(_.value).toSeq
      }

    }.repeat
  }
}
