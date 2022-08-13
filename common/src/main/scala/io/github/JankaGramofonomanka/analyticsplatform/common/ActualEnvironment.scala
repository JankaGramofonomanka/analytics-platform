package io.github.JankaGramofonomanka.analyticsplatform.common

import io.github.JankaGramofonomanka.analyticsplatform.common.Environment
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils

object ActualEnvironment extends Environment {

  val NUM_TAGS_TO_KEEP  = Utils.getEnvVarInt("NUM_TAGS_TO_KEEP")
  val DEFAULT_LIMIT     = Utils.getEnvVarInt("DEFAULT_LIMIT")
  
  val AEROSPIKE_HOSTNAME  = Utils.getEnvVar("AEROSPIKE_HOSTNAME")
  val AEROSPIKE_PORT      = Utils.getEnvVarInt("AEROSPIKE_PORT")
  
  val KAFKA_TOPIC               = Utils.getEnvVar("KAFKA_TOPIC")
  val KAFKA_BOOTSTRAP_SERVERS   = Utils.getEnvVar("KAFKA_BOOTSTRAP_SERVERS")
  val KAFKA_GROUP_ID            = Utils.getEnvVar("KAFKA_GROUP")
  val KAFKA_CLIENT_ID           = Utils.getEnvVar("KAFKA_CONSUMER_ID")
  val KAFKA_POLL_TIMEOUT_MILLIS = Utils.getEnvVarInt("KAFKA_POLL_TIMEOUT").toLong
  
  val FRONTEND_HOSTNAME = Utils.getEnvVar("FRONTEND_HOSTNAME")
  val FRONTEND_PORT     = Utils.getEnvVarInt("FRONTEND_PORT")

}
