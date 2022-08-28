package io.github.JankaGramofonomanka.analyticsplatform.tagprocessor


import java.util.Properties

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.consumer._

import io.github.JankaGramofonomanka.analyticsplatform.common.{Config => CommonConfig}
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.codecs.KafkaCodec.UserTagDeserializer

object Config {

  val Common = CommonConfig

  trait Environment extends Common.Environment {

    val NUM_TAGS_TO_KEEP:           Int
    val KAFKA_GROUP_ID:             String
    val KAFKA_CLIENT_ID:            String
    val KAFKA_POLL_TIMEOUT_MILLIS:  Long
    val KAFKA_MAX_POLL_RECORDS:     Int
    val MAX_PARALLEL_WRITES:        Int
    
  }

  class ActualEnvironment extends Common.ActualEnvironment with Environment {

    val NUM_TAGS_TO_KEEP          = Utils.getEnvVarOptionInt("NUM_TAGS_TO_KEEP").getOrElse(Defaults.NUM_TAGS_TO_KEEP)
    val KAFKA_GROUP_ID            = Utils
                                      .getEnvVarOption("KAFKA_GROUP")
                                      .getOrElse(Defaults.KAFKA_GROUP_ID)
    val KAFKA_CLIENT_ID           = Utils
                                      .getEnvVarOption("KAFKA_CONSUMER_ID")
                                      .getOrElse(Defaults.KAFKA_CLIENT_ID)
    val KAFKA_POLL_TIMEOUT_MILLIS = Utils
                                      .getEnvVarOptionInt("KAFKA_POLL_TIMEOUT")
                                      .getOrElse(Defaults.KAFKA_POLL_TIMEOUT_MILLIS)
                                      .toLong

    val KAFKA_MAX_POLL_RECORDS    = Utils
                                      .getEnvVarOptionInt("KAFKA_MAX_POLL_RECORDS")
                                      .getOrElse(Defaults.KAFKA_MAX_POLL_RECORDS)

    val MAX_PARALLEL_WRITES       = Utils
                                      .getEnvVarOptionInt("MAX_PARALLEL_WRITES")
                                      .getOrElse(Defaults.MAX_PARALLEL_WRITES)

    
    private object Defaults {
      val NUM_TAGS_TO_KEEP          = 200
      val KAFKA_GROUP_ID            = "aggregate-processors"
      val KAFKA_CLIENT_ID           = "consumer"
      val KAFKA_POLL_TIMEOUT_MILLIS = 1000
      val KAFKA_MAX_POLL_RECORDS    = 500
      val MAX_PARALLEL_WRITES       = 10
    }
    
  }

  object Kafka {

    def getConsumerProps(implicit env: Environment): Properties = {
      val props = new Properties()
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.KAFKA_BOOTSTRAP_SERVERS)
      
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   classOf[StringDeserializer])
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[UserTagDeserializer])
      props.put(ConsumerConfig.GROUP_ID_CONFIG,                 env.KAFKA_GROUP_ID)
      props.put(ConsumerConfig.CLIENT_ID_CONFIG,                env.KAFKA_CLIENT_ID)
      props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,         env.KAFKA_MAX_POLL_RECORDS)

      props
    }
  }
}


