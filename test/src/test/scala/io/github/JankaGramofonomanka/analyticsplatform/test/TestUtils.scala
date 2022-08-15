package io.github.JankaGramofonomanka.analyticsplatform.test

import scala.collection.mutable.{Queue, Map}

import cats.effect._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.frontend.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Config.{Environment => FrontendEnv}
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.AggregateProcessorOps
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.Config.{Environment => ProcEnv}
import io.github.JankaGramofonomanka.analyticsplatform.test.Mock




object TestUtils {

  object MockEnv extends FrontendEnv with ProcEnv {
    val NUM_TAGS_TO_KEEP  = 200
    val DEFAULT_LIMIT     = 200

    val AEROSPIKE_HOSTNAME  = "localhost"
    val AEROSPIKE_PORT      = 3000

    val AEROSPIKE_NAMESPACE       = "analyticsplatform"
    val AEROSPIKE_PROFILES_SET    = "profiles"
    val AEROSPIKE_AGGREGATES_SET  = "aggregates"
    val AEROSPIKE_PROFILES_BIN    = "profile"
    val AEROSPIKE_AGGREGATES_BIN  = "aggregate"

    val KAFKA_TOPIC             = "topic"
    val KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
    val KAFKA_GROUP_ID          = "kafka-group"
    val KAFKA_CLIENT_ID         = "kafka-client"
    
    val KAFKA_POLL_TIMEOUT_MILLIS = 1

    val FRONTEND_HOSTNAME = "localhost"
    val FRONTEND_PORT     = 8080

    val USE_LOGGER  = true
    val LOG_HEADERS = false
    val LOG_BODY    = false
  }

  final case class Storage(
    profiles:   Map[Cookie, TrackGen[SimpleProfile]],
    aggregates: Map[AggregateKey, TrackGen[AggregateValue]],
    queue:      Queue[UserTag],
  )

  object Storage {
    def empty: Storage = Storage(Map(), Map(), new Queue)
  }

  final case class StorageInterface[F[_]](
    profiles:   KeyValueDB[F, Cookie, SimpleProfile],
    aggregates: KeyValueDB[F, AggregateKey, AggregateValue],
    publisher:  Topic.Publisher[F, UserTag],
    subscriber: Topic.Subscriber[F, UserTag],
  )

  def getMocks(storage: Storage): StorageInterface[IO] = {
    val db    = new Mock.DB(storage.profiles, storage.aggregates)
    val topic = new Mock.Topic(storage.queue)
    StorageInterface(db.Profiles, db.Aggregates, topic.Publisher, topic.Subscriber)
  }

  def getOps[F[_]: Sync](interface: StorageInterface[F]): (FrontendOps[F], AggregateProcessorOps[F]) = {
    implicit val env = MockEnv
    val frontend            = new FrontendOps[F](interface.profiles, interface.aggregates, interface.publisher)
    val aggregateProcessor  = new AggregateProcessorOps[F](interface.aggregates, interface.subscriber)
    (frontend, aggregateProcessor)
  }

  def getTimeRangeContaining(timestamp: Timestamp): TimeRange = {
    val from  = timestamp.getBucket.toDateTime
    val to    = timestamp.getBucket.addMinutes(1).toDateTime
    TimeRange(from, to)
  }
}

