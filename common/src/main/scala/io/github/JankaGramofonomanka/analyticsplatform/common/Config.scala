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
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.KafkaCodec._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.Environment

object Config {

  object Query {

    object ParamNames {
      val limit       = "limit"
      val timeRange   = "time_range"
      val action      = "action"
      val aggregates  = "aggregates"
      val origin      = "origin"
      val brandId     = "brand_id"
      val categoryId  = "category_id"
    }
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
    def getConfig: AerospikeConfig = {
    
      val writePolicy = new WritePolicy()
      writePolicy.generationPolicy = EXPECT_GEN_EQUAL

      // TODO specify policies
      AerospikeConfig(
        new Policy(),
        writePolicy,
        "analyticsplatform",
        "profiles",
        "aggregates",
        "profile",
        "aggregate",
      )
      
    }

    def getClient(implicit env: Environment)
      = new AerospikeClient(env.AEROSPIKE_HOSTNAME, env.AEROSPIKE_PORT)

  }
  
  object Kafka {

    def getProducerProps(implicit env: Environment): Properties = {
      val props = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.KAFKA_BOOTSTRAP_SERVERS)
  
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   classOf[StringSerializer])
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[UserTagSerializer])

      props
    }

    def getConsumerProps(implicit env: Environment): Properties = {
      val props = new Properties()
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.KAFKA_BOOTSTRAP_SERVERS)
      
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   classOf[StringDeserializer])
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[UserTagDeserializer])
      props.put(ConsumerConfig.GROUP_ID_CONFIG,                 env.KAFKA_GROUP_ID)
      props.put(ConsumerConfig.CLIENT_ID_CONFIG,                env.KAFKA_CLIENT_ID)

      props
    }
  }

  object Frontend {
    
    def getHost(implicit env: Environment): Ipv4Address
      = Ipv4Address.fromString(env.FRONTEND_HOSTNAME)
        .getOrElse(throw new Utils.InvalidEnvironmentVariableException("Could not parse hostname"))
    
    def getPort(implicit env: Environment): Port
      = Port.fromInt(env.FRONTEND_PORT)
        .getOrElse(throw new Utils.InvalidEnvironmentVariableException("Could not parse port"))
  }
}


