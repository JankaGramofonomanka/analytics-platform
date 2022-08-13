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

    val AEROSPIKE_HOSTNAME        = Utils.getEnvVar("AEROSPIKE_HOSTNAME")
    val AEROSPIKE_PORT            = Utils.getEnvVarInt("AEROSPIKE_PORT")
    val AEROSPIKE_NAMESPACE       = Utils.getEnvVar("AEROSPIKE_NAMESPACE")
    val AEROSPIKE_PROFILES_SET    = Utils.getEnvVar("AEROSPIKE_PROFILES_SET")
    val AEROSPIKE_AGGREGATES_SET  = Utils.getEnvVar("AEROSPIKE_AGGREGATES_SET")
    val AEROSPIKE_PROFILES_BIN    = Utils.getEnvVar("AEROSPIKE_PROFILES_BIN")
    val AEROSPIKE_AGGREGATES_BIN  = Utils.getEnvVar("AEROSPIKE_AGGREGATES_BIN")

    
    val KAFKA_TOPIC               = Utils.getEnvVar("KAFKA_TOPIC")
    val KAFKA_BOOTSTRAP_SERVERS   = Utils.getEnvVar("KAFKA_BOOTSTRAP_SERVERS")
    
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


