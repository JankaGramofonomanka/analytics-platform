package io.github.JankaGramofonomanka.analyticsplatform.common

import scala.collection.mutable.{Queue, Map}

import cats.effect._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.{FrontendOps, AggregateProcessorOps}
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.{Mock => MockDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.{Mock => MockTopic, Topic}



object TestUtils {
  final case class Storage(
    profiles:   Map[Cookie, TrackGen[SimpleProfile]],
    aggregates: Map[AggregateKey, TrackGen[AggregateValue]],
    queue:      Queue[UserTag],
  )

  object Storage {
    def empty: Storage = Storage(Map(), Map(), new Queue)
  }

  final case class StorageInterface[F[_]](
    profiles:   ProfilesDB[F],
    aggregates: AggregatesDB[F],
    publisher:  Topic.Publisher[F, UserTag],
    subscriber: Topic.Subscriber[F, UserTag],
  )

  def getMocks(storage: Storage): StorageInterface[IO] = {
    val db    = new MockDB(storage.profiles, storage.aggregates)
    val topic = new MockTopic(storage.queue)
    StorageInterface(db, db, topic.Publisher, topic.Subscriber)
  }

  def getOps[F[_]: Sync](interface: StorageInterface[F]): (FrontendOps[F], AggregateProcessorOps[F]) = {
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

