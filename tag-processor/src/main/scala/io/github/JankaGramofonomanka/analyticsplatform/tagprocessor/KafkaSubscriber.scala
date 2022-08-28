package io.github.JankaGramofonomanka.analyticsplatform.tagprocessor

import cats.effect.IO
import fs2.Stream

import org.apache.kafka.clients.consumer._

import java.time.Duration
import scala.jdk.CollectionConverters._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.Config.Environment

class KafkaSubscriber(consumer: KafkaConsumer[Nothing, UserTag])(implicit env: Environment)
extends Topic.Subscriber[IO, UserTag] {
  
  def subscribe: Stream[IO, Seq[UserTag]] = Stream.eval {
  
    IO.delay {
      val records = consumer.poll(Duration.ofMillis(env.KAFKA_POLL_TIMEOUT_MILLIS))
      records.asScala.map(_.value).toSeq
    }

  }.repeat
}

