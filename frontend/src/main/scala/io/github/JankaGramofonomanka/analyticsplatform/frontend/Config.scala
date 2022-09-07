package io.github.JankaGramofonomanka.analyticsplatform.frontend


import com.comcast.ip4s.{Ipv4Address, Port}
import java.util.Properties

import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.clients.producer._

import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.{Config => CommonConfig}
import io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs.KafkaCodec.UserTagSerializer


object Config {

  val Common = CommonConfig

  trait Environment extends Common.Environment {

    val DEFAULT_LIMIT:    Int

    val FRONTEND_HOSTNAME:  String
    val FRONTEND_PORT:      Int

    val USE_LOGGER:   Boolean
    val LOG_HEADERS:  Boolean
    val LOG_BODY:     Boolean
  }

  class ActualEnvironment extends Common.ActualEnvironment with Environment {

    val DEFAULT_LIMIT     = Utils.getEnvVarOptionInt("DEFAULT_LIMIT")   .getOrElse(Defaults.DEFAULT_LIMIT)
    
    val FRONTEND_HOSTNAME = Utils.getEnvVarOption("FRONTEND_HOSTNAME").getOrElse(Defaults.FRONTEND_HOSTNAME)
    val FRONTEND_PORT     = Utils.getEnvVarOptionInt("FRONTEND_PORT") .getOrElse(Defaults.FRONTEND_PORT)

    val USE_LOGGER        = Utils.getEnvVarBoolean("USE_LOGGER",  Defaults.USE_LOGGER)
    val LOG_HEADERS       = Utils.getEnvVarBoolean("LOG_HEADERS", Defaults.LOG_HEADERS)
    val LOG_BODY          = Utils.getEnvVarBoolean("LOG_BODY",    Defaults.LOG_BODY)

    private object Defaults {
      val DEFAULT_LIMIT     = 200
      
      val FRONTEND_HOSTNAME = "0.0.0.0"
      val FRONTEND_PORT     = 8080

      val USE_LOGGER        = true
      val LOG_HEADERS       = false
      val LOG_BODY          = false
    }
  }


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

  object Kafka {

    def getProducerProps(implicit env: Environment): Properties = {
      val props = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.KAFKA_BOOTSTRAP_SERVERS)
  
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   classOf[StringSerializer])
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[UserTagSerializer])

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


