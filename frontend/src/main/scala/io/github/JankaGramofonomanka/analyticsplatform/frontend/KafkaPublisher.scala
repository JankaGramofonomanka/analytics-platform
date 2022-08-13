package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.IO

import org.apache.kafka.clients.producer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic

class KafkaPublisher(producer: KafkaProducer[Nothing, UserTag], topicName: String)
extends Topic.Publisher[IO, UserTag] {

  def publish(tag: UserTag): IO[Unit] = IO.delay {

    val record = new ProducerRecord(topicName, tag)
    producer.send(record).get
    ()
  }
}
