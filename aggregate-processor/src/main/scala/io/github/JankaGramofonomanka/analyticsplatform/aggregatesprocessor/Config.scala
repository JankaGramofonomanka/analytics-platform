package io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor


import java.util.Properties

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.consumer._

import io.github.JankaGramofonomanka.analyticsplatform.common.{Config => CommonConfig}
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.codecs.KafkaCodec.UserTagDeserializer

object Config {

  val Common = CommonConfig

  trait Environment extends Common.Environment {

    val KAFKA_GROUP_ID:           String
    val KAFKA_CLIENT_ID:          String
    val KAFKA_POLL_TIMEOUT_MILLIS: Long

  }

  class ActualEnvironment extends Common.ActualEnvironment with Environment {

    val KAFKA_GROUP_ID            = Utils.getEnvVar("KAFKA_GROUP")
    val KAFKA_CLIENT_ID           = Utils.getEnvVar("KAFKA_CONSUMER_ID")
    val KAFKA_POLL_TIMEOUT_MILLIS = Utils.getEnvVarInt("KAFKA_POLL_TIMEOUT").toLong
    
  }

  object Kafka {

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
}

