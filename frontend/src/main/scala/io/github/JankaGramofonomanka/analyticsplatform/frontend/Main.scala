package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.{ExitCode, IO, IOApp}

import org.apache.kafka.clients.producer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.FrontendServer
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.Aerospike
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.KafkaTopic
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.IOEntityCodec

object Main extends IOApp {
  def run(args: List[String]) = {
    val db = new Aerospike.DB(Config.Aerospike.client, Config.Aerospike.config)
    
    val producer = new KafkaProducer[Nothing, UserTag](Config.Kafka.getProducerProps)
    
    val tagsToAggregate = new KafkaTopic.Publisher(producer, Config.Kafka.TOPIC)

    FrontendServer.stream[IO](
      db.Profiles,
      db.Aggregates,
      tagsToAggregate,
      IOEntityCodec
    ).compile.drain.as(ExitCode.Success)
  }
}
