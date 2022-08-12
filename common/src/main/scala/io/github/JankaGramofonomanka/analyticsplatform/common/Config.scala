package io.github.JankaGramofonomanka.analyticsplatform.common


import com.comcast.ip4s._
import java.util.Properties

import org.apache.kafka.common.serialization.{StringSerializer, StringDeserializer}
import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.consumer._

import com.aerospike.client.policy.{Policy, WritePolicy}
import com.aerospike.client.policy.GenerationPolicy._
import com.aerospike.client.AerospikeClient

import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.Aerospike.{Config => AerospikeConfig}
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.Kafka._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils

object Config {

  object Other {
    val numTagsToKeep = Utils.getEnvVarInt("NUM_TAGS_TO_KEEP")
  }

  object QueryParams {
    val limit       = "limit"
    val timeRange   = "time_range"
    val action      = "action"
    val aggregates  = "aggregates"
    val origin      = "origin"
    val brandId     = "brand_id"
    val categoryId  = "category_id"

    val defaultLimit = Utils.getEnvVarInt("DEFAULT_LIMIT")
  }

  object Aggregates {
    object Fields {

      val columns = "columns"
      val rows    = "rows"

      val bucket      = "1m_bucket"
      val action      = "action"
      val origin      = "origin"
      val brandId     = "brand_id"
      val categoryId  = "category_id"
      val sumPrice    = "sum_price"
      val count       = "count"
    }
  }
    
  object Aerospike {
    
    private val HOSTNAME = Utils.getEnvVar("AEROSPIKE_HOSTNAME")
    private val PORT: Int = Utils.getEnvVarInt("AEROSPIKE_PORT")
    
    private val writePolicy = new WritePolicy()
    writePolicy.generationPolicy = EXPECT_GEN_EQUAL

    // TODO specify policies
    val config = AerospikeConfig(
      new Policy(),
      writePolicy,
      "analyticsplatform",
      "profiles",
      "aggregates",
      "profile",
      "aggregate",
    )
    val client = new AerospikeClient(HOSTNAME, PORT)

  }

  object Kafka {

    val TOPIC = Utils.getEnvVar("KAFKA_TOPIC")
    private val BOOTSTRAP_SERVERS = Utils.getEnvVar("KAFKA_BOOTSTRAP_SERVERS")
    private val GROUP_ID = Utils.getEnvVar("KAFKA_GROUP")
    private val CLIENT_ID = Utils.getEnvVar("KAFKA_CONSUMER_ID")
    
    val pollTimeoutMillis: Long = Utils.getEnvVarInt("KAFKA_POLL_TIMEOUT").toLong
    
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
      props.put(ConsumerConfig.GROUP_ID_CONFIG,                 GROUP_ID)
      props.put(ConsumerConfig.CLIENT_ID_CONFIG,                CLIENT_ID)

      props
    }
  }

  object Frontend {
    
    val host = Ipv4Address.fromString(Utils.getEnvVar("FRONTEND_HOSTNAME"))
      .getOrElse(throw new Utils.InvalidEnvironmentVariableException("Could not parse hostname"))
    val port = Port.fromInt(Utils.getEnvVarInt("FRONTEND_PORT"))
      .getOrElse(throw new Utils.InvalidEnvironmentVariableException("Could not parse port"))
  }
}
