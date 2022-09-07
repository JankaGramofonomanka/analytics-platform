package io.github.JankaGramofonomanka.analyticsplatform.common


import com.aerospike.client.policy.{ClientPolicy, WritePolicy}
import com.aerospike.client.policy.GenerationPolicy._
import com.aerospike.client.policy.CommitLevel._
import com.aerospike.client.AerospikeClient

import io.github.JankaGramofonomanka.analyticsplatform.common.Utils

object Config {

  trait Environment {

    val AEROSPIKE_HOSTNAME:             String
    val AEROSPIKE_PORT:                 Int
    val AEROSPIKE_PROFILES_NAMESPACE:   String
    val AEROSPIKE_AGGREGATES_NAMESPACE: String
    val AEROSPIKE_PROFILES_BIN:         String
    val AEROSPIKE_COMMIT_LEVEL:         String
    val AEROSPIKE_GENERATION_POLICY:    String
    val AEROSPIKE_BUCKETS_PER_KEY:      Int

    val KAFKA_TOPIC:              String
    val KAFKA_BOOTSTRAP_SERVERS:  String
    
  }

  class ActualEnvironment extends Environment {

    val AEROSPIKE_HOSTNAME              = Utils
                                            .getEnvVarOption("AEROSPIKE_HOSTNAME")
                                            .getOrElse(Defaults.AEROSPIKE_HOSTNAME)
    val AEROSPIKE_PORT                  = Utils
                                            .getEnvVarOptionInt("AEROSPIKE_PORT")
                                            .getOrElse(Defaults.AEROSPIKE_PORT)
    
    val AEROSPIKE_PROFILES_NAMESPACE    = Utils
                                            .getEnvVarOption("AEROSPIKE_PROFILES_NAMESPACE")
                                            .getOrElse(Defaults.AEROSPIKE_PROFILES_NAMESPACE)
    val AEROSPIKE_AGGREGATES_NAMESPACE  = Utils
                                            .getEnvVarOption("AEROSPIKE_AGGREGATES_NAMESPACE")
                                            .getOrElse(Defaults.AEROSPIKE_AGGREGATES_NAMESPACE)
    val AEROSPIKE_PROFILES_BIN          = Utils
                                            .getEnvVarOption("AEROSPIKE_PROFILES_BIN")
                                            .getOrElse(Defaults.AEROSPIKE_PROFILES_BIN)
    val AEROSPIKE_COMMIT_LEVEL          = Utils
                                            .getEnvVarOption("AEROSPIKE_COMMIT_LEVEL")
                                            .getOrElse(Defaults.AEROSPIKE_COMMIT_LEVEL)

    val AEROSPIKE_GENERATION_POLICY     = Utils
                                            .getEnvVarOption("AEROSPIKE_GENERATION_POLICY")
                                            .getOrElse(Defaults.AEROSPIKE_GENERATION_POLICY)

    val AEROSPIKE_BUCKETS_PER_KEY       = Utils
                                            .getEnvVarOptionInt("AEROSPIKE_BUCKETS_PER_KEY")
                                            .getOrElse(Defaults.AEROSPIKE_BUCKETS_PER_KEY)
    
    val KAFKA_TOPIC               = Utils
                                      .getEnvVarOption("KAFKA_TOPIC")
                                      .getOrElse(Defaults.KAFKA_TOPIC)
    val KAFKA_BOOTSTRAP_SERVERS   = Utils
                                      .getEnvVarOption("KAFKA_BOOTSTRAP_SERVERS")
                                      .getOrElse(Defaults.KAFKA_BOOTSTRAP_SERVERS)


    private object Defaults {
      val AEROSPIKE_HOSTNAME              = "localhost"
      val AEROSPIKE_PORT                  = 3000
      val AEROSPIKE_PROFILES_NAMESPACE    = "profiles"
      val AEROSPIKE_AGGREGATES_NAMESPACE  = "aggregates"
      val AEROSPIKE_PROFILES_BIN          = ""
      val AEROSPIKE_COMMIT_LEVEL          = "ALL"
      val AEROSPIKE_GENERATION_POLICY     = "EQ"
      val AEROSPIKE_BUCKETS_PER_KEY       = 60

      val KAFKA_TOPIC               = "tags"
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
      writePolicy.generationPolicy = env.AEROSPIKE_GENERATION_POLICY match {
        case "EQ"   => EXPECT_GEN_EQUAL
        case "GT"   => EXPECT_GEN_GT
        case "NONE" => NONE
        case _      => NONE
      } 

      writePolicy.commitLevel = env.AEROSPIKE_COMMIT_LEVEL match {
        case "MASTER" => COMMIT_MASTER
        case "ALL"    => COMMIT_ALL
        case _        => COMMIT_MASTER

      }
      clientPolicy.writePolicyDefault = writePolicy

      new AerospikeClient(clientPolicy, env.AEROSPIKE_HOSTNAME, env.AEROSPIKE_PORT)
    }

  }
  
}


