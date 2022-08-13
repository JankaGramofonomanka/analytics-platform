package io.github.JankaGramofonomanka.analyticsplatform.common

trait Environment {

  val NUM_TAGS_TO_KEEP: Int
  val DEFAULT_LIMIT:    Int

  val AEROSPIKE_HOSTNAME: String
  val AEROSPIKE_PORT:     Int

  val KAFKA_TOPIC:              String
  val KAFKA_BOOTSTRAP_SERVERS:  String
  val KAFKA_GROUP_ID:           String
  val KAFKA_CLIENT_ID:          String
  
  val KAFKA_POLL_TIMEOUT_MILLIS: Long

  val FRONTEND_HOSTNAME:  String
  val FRONTEND_PORT:      Int
}

