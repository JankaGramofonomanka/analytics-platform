package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.{ExitCode, IO, IOApp}

import org.apache.kafka.clients.producer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.Environment
import io.github.JankaGramofonomanka.analyticsplatform.common.ActualEnvironment
import io.github.JankaGramofonomanka.analyticsplatform.common.FrontendServer
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.IOEntityCodec
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.Aerospike
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.KafkaTopic

object Main extends IOApp {
  def run(args: List[String]) = {

    implicit val env: Environment = ActualEnvironment


    val db = new Aerospike.DB(Config.Aerospike.getClient, Config.Aerospike.getConfig)
    
    val producer = new KafkaProducer[Nothing, UserTag](Config.Kafka.getProducerProps)
    
    val tagsToAggregate = new KafkaTopic.Publisher(producer, env.KAFKA_TOPIC)


    implicit val codec = IOEntityCodec
    FrontendServer.stream[IO](
      db.Profiles,
      db.Aggregates,
      tagsToAggregate,
      
    ).compile.drain.as(ExitCode.Success)
  }
}
