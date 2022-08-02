package io.github.JankaGramofonomanka.analyticsplatform.common

import java.util.Properties

import org.apache.kafka.common.serialization.{StringSerializer, StringDeserializer}
import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.consumer._

import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Aerospike.{Config => AerospikeConfig}
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.Kafka._

object Config {
    
  object Aerospike {
    
    import com.aerospike.client.policy.{Policy, WritePolicy, ClientPolicy}
    import com.aerospike.client.{AerospikeClient, Host}

    // TODO Move literals somewhere
    private val host = new Host("localhost", 3000)

    // TODO specify policies
    val config = AerospikeConfig(
      new Policy(),
      new WritePolicy(),
      "analyticsplatform",
      "profiles",
      "aggregates",
      "profile",
      "aggregate",
    )
    val client = new AerospikeClient(new ClientPolicy(), host)

  }

  object Kafka {
    val TOPIC = "test"
    private val BOOTSTRAP_SERVERS = "localhost:9092"
    

    def getProducerProps: Properties = {
      val props = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
  
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   classOf[StringSerializer])
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[UserTagSerializer])

      props
    }

    def getConsumerProps: Properties = {
      val props = new Properties()
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
      
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   classOf[StringDeserializer])
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[UserTagDeserializer])
      props.put("group.id", "something")

      props
    }
      
  }
}
