package io.github.JankaGramofonomanka.analyticsplatform.common


import com.aerospike.client.policy.{Policy, WritePolicy}
import com.aerospike.client.policy.GenerationPolicy._
import com.aerospike.client.AerospikeClient

import io.github.JankaGramofonomanka.analyticsplatform.common.Aerospike.{Config => AerospikeConfig}
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils

object Config {

  trait Environment {

    val AEROSPIKE_HOSTNAME: String
    val AEROSPIKE_PORT:     Int

    val KAFKA_TOPIC:              String
    val KAFKA_BOOTSTRAP_SERVERS:  String
    
  }

  class ActualEnvironment extends Environment {

    val AEROSPIKE_HOSTNAME  = Utils.getEnvVar("AEROSPIKE_HOSTNAME")
    val AEROSPIKE_PORT      = Utils.getEnvVarInt("AEROSPIKE_PORT")
    
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
  
}


