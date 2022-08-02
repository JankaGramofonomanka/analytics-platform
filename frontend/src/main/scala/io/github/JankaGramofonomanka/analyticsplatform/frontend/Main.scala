package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.{ExitCode, IO, IOApp}

import java.util.Properties
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer

import com.aerospike.client.{AerospikeClient, Host}
import com.aerospike.client.policy.{Policy, WritePolicy, ClientPolicy}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Mock
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Aerospike
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.KafkaTopic
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.IOEntityCodec
import io.github.JankaGramofonomanka.analyticsplatform.common.FrontendServer
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.Kafka.UserTagSerializer

object Main extends IOApp {
  def run(args: List[String]) =
    {
      // TODO Move literals somewhere
      val host = new Host("localhost", 3000)

      // TODO specify policies
      val config = Aerospike.Config(
        new Policy(),
        new WritePolicy(),
        "analyticsplatform",
        "profiles",
        "aggregates",
        "profile",
        "aggregate",
      )
      val client = new AerospikeClient(new ClientPolicy(), host)
      val profiles = new Aerospike.DB(client, config)


      // TODO move these few lines somewhere
      val TOPIC = "test"

      val props = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   classOf[StringSerializer])
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[UserTagSerializer])

      val producer = new KafkaProducer[String, UserTag](props)
      val tagsToAggregate = new KafkaTopic.Publisher(producer, TOPIC)

      FrontendServer.stream[IO](profiles, Mock.DB, tagsToAggregate, IOEntityCodec)
        .compile.drain.as(ExitCode.Success)
    }
}
