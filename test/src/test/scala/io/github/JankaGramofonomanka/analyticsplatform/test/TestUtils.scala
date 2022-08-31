package io.github.JankaGramofonomanka.analyticsplatform.test

import scala.collection.mutable.{Queue, Map}

import cats.effect._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.frontend.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Config.{Environment => FrontendEnv}
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.TagProcessorOps
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.Config.{Environment => ProcEnv}
import io.github.JankaGramofonomanka.analyticsplatform.test.Mock




object TestUtils {

  def getTags(action: Action, profile: Profile): Vector[UserTag] = action match {
    case VIEW => profile.views
    case BUY  => profile.buys
  }

  def fromTags(cookie: Cookie, tags: Seq[UserTag]): Profile = {
    val views = tags.filter(_.action == VIEW).toVector
    val buys  = tags.filter(_.action == BUY).toVector
    Profile(cookie, views, buys)
  }

  def profileContains(profile: Profile, tag: UserTag): Boolean
    = profile.views.contains(tag) || profile.buys.contains(tag)

  object MockEnv extends FrontendEnv with ProcEnv {
    val NUM_TAGS_TO_KEEP  = 200
    val DEFAULT_LIMIT     = 200

    val AEROSPIKE_HOSTNAME  = "localhost"
    val AEROSPIKE_PORT      = 3000

    val AEROSPIKE_PROFILES_NAMESPACE    = "profiles"
    val AEROSPIKE_AGGREGATES_NAMESPACE  = "aggregates"
    val AEROSPIKE_PROFILES_BIN          = ""
    val AEROSPIKE_AGGREGATES_BIN        = ""
    val AEROSPIKE_COMMIT_LEVEL          = "ALL"
    val AEROSPIKE_GENERATION_POLICY     = "EQ"
    val AEROSPIKE_BUCKETS_PER_KEY       = 10

    val KAFKA_TOPIC             = "topic"
    val KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
    val KAFKA_GROUP_ID          = "kafka-group"
    val KAFKA_CLIENT_ID         = "kafka-client"
    val KAFKA_MAX_POLL_RECORDS  = 500
    val MAX_PARALLEL_WRITES     = 10
    
    val KAFKA_POLL_TIMEOUT_MILLIS = 1

    val FRONTEND_HOSTNAME = "localhost"
    val FRONTEND_PORT     = 8080

    val USE_LOGGER  = true
    val LOG_HEADERS = false
    val LOG_BODY    = false
  }

  final case class Storage(
    profiles:   Map[Cookie, TrackGen[Profile]],
    aggregates: Map[AggregateKey, TrackGen[AggregateVB]],
    queue:      Queue[UserTag],
  )

  object Storage {
    def empty: Storage = Storage(Map(), Map(), new Queue)
  }

  final case class StorageInterface[F[_]](
    profiles:   KeyValueDB[F, Cookie, Profile],
    aggregates: KeyValueDB[F, AggregateKey, AggregateVB],
    publisher:  Topic.Publisher[F, UserTag],
    subscriber: Topic.Subscriber[F, UserTag],
  )

  def getMocks(storage: Storage): StorageInterface[IO] = {
    val db    = new Mock.DB(storage.profiles, storage.aggregates)
    val topic = new Mock.Topic(storage.queue)
    StorageInterface(db.Profiles, db.Aggregates, topic.Publisher, topic.Subscriber)
  }

  def getOps[F[_]: Async](interface: StorageInterface[F]): (FrontendOps[F], TagProcessorOps[F]) = {
    implicit val env = MockEnv
    val frontend            = new FrontendOps[F](interface.profiles, interface.aggregates, interface.publisher)
    val aggregateProcessor  = new TagProcessorOps[F](interface.profiles, interface.aggregates, interface.subscriber)
    (frontend, aggregateProcessor)
  }

  def getTimeRangeContaining(timestamp: Timestamp): TimeRange = {
    val from  = timestamp.getBucket.toDateTime
    val to    = timestamp.getBucket.addMinutes(1).toDateTime
    TimeRange(from, to)
  }
}

