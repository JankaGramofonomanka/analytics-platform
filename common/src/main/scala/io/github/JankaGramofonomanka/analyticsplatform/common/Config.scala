package io.github.JankaGramofonomanka.analyticsplatform.common


import com.aerospike.client.policy.{ClientPolicy, WritePolicy}
import com.aerospike.client.policy.GenerationPolicy._
import com.aerospike.client.AerospikeClient

import io.github.JankaGramofonomanka.analyticsplatform.common.Utils

object Config {

  trait Environment {

    val AEROSPIKE_HOSTNAME:       String
    val AEROSPIKE_PORT:           Int
    val AEROSPIKE_NAMESPACE:      String
    val AEROSPIKE_PROFILES_SET:   String
    val AEROSPIKE_AGGREGATES_SET: String
    val AEROSPIKE_PROFILES_BIN:   String
    val AEROSPIKE_AGGREGATES_BIN: String

    val KAFKA_TOPIC:              String
    val KAFKA_BOOTSTRAP_SERVERS:  String
    
  }

  class ActualEnvironment extends Environment {

    val AEROSPIKE_HOSTNAME        = Utils
                                      .getEnvVarOption("AEROSPIKE_HOSTNAME")
                                      .getOrElse(Defaults.AEROSPIKE_HOSTNAME)
    val AEROSPIKE_PORT            = Utils
                                      .getEnvVarOptionInt("AEROSPIKE_PORT")
                                      .getOrElse(Defaults.AEROSPIKE_PORT)
    val AEROSPIKE_NAMESPACE       = Utils
                                      .getEnvVarOption("AEROSPIKE_NAMESPACE")
                                      .getOrElse(Defaults.AEROSPIKE_NAMESPACE)
    val AEROSPIKE_PROFILES_SET    = Utils
                                      .getEnvVarOption("AEROSPIKE_PROFILES_SET")
                                      .getOrElse(Defaults.AEROSPIKE_PROFILES_SET)
    val AEROSPIKE_AGGREGATES_SET  = Utils
                                      .getEnvVarOption("AEROSPIKE_AGGREGATES_SET")
                                      .getOrElse(Defaults.AEROSPIKE_AGGREGATES_SET)
    val AEROSPIKE_PROFILES_BIN    = Utils
                                      .getEnvVarOption("AEROSPIKE_PROFILES_BIN")
                                      .getOrElse(Defaults.AEROSPIKE_PROFILES_BIN)
    val AEROSPIKE_AGGREGATES_BIN  = Utils
                                      .getEnvVarOption("AEROSPIKE_AGGREGATES_BIN")
                                      .getOrElse(Defaults.AEROSPIKE_AGGREGATES_BIN)

    
    val KAFKA_TOPIC               = Utils
                                      .getEnvVarOption("KAFKA_TOPIC")
                                      .getOrElse(Defaults.KAFKA_TOPIC)
    val KAFKA_BOOTSTRAP_SERVERS   = Utils
                                      .getEnvVarOption("KAFKA_BOOTSTRAP_SERVERS")
                                      .getOrElse(Defaults.KAFKA_BOOTSTRAP_SERVERS)


    private object Defaults {
      val AEROSPIKE_HOSTNAME        = "localhost"
      val AEROSPIKE_PORT            = 3000
      val AEROSPIKE_NAMESPACE       = "analyticsplatform"
      val AEROSPIKE_PROFILES_SET    = "profiles"
      val AEROSPIKE_AGGREGATES_SET  = "aggregates"
      val AEROSPIKE_PROFILES_BIN    = "profile"
      val AEROSPIKE_AGGREGATES_BIN  = "aggregate"

      val KAFKA_TOPIC               = "tags-to-aggregate"
      val KAFKA_BOOTSTRAP_SERVERS   = "localhost:9092"
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

    def getClient(implicit env: Environment) = {
      val clientPolicy = new ClientPolicy()

      val writePolicy = new WritePolicy()
      writePolicy.generationPolicy = EXPECT_GEN_EQUAL
      clientPolicy.writePolicyDefault = writePolicy

      new AerospikeClient(clientPolicy, env.AEROSPIKE_HOSTNAME, env.AEROSPIKE_PORT)
    }
      

  }
  
}


