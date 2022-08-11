package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import cats.effect.Sync
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic

class FrontendOps[F[_]: Sync](
  profiles: ProfilesDB[F],
  aggregates: AggregatesDB[F],
  tagsToAggregate: Topic.Publisher[F, UserTag]
) {
  
  private def tryStoreTag(tag: UserTag): F[Boolean] = for {
    profile <- profiles.getProfile(tag.cookie)
    updated = profile.map(_.update(tag, Config.Other.numTagsToKeep))
    result <- profiles.updateProfile(tag.cookie, updated)
  } yield result

  def storeTag(tag: UserTag): F[Unit] = for {
    _ <- Utils.tryTillSuccess(tryStoreTag(tag))
    _ <- tagsToAggregate.publish(tag)
  } yield ()


  def getProfile(cookie: Cookie, timeRange: TimeRange, limit: Limit): F[PrettyProfile] = for {
    profile <- profiles.getProfile(cookie)
    tags = profile.value.tags.filter(tag => timeRange.contains(tag.time)).take(limit)
  } yield SimpleProfile(tags).prettify(cookie)

  def getAggregates(
      timeRange:  TimeRange,
      fields:     AggregateFields,
  ): F[Aggregates] = {
    
      val buckets = timeRange.getBuckets
      val keys = buckets.map(bucket => AggregateKey.fromFields(bucket, fields))
      for {
        aggregateValues <- keys.traverse { key => aggregates.getAggregate(key).map(_.value) }
        
        values = for {
          (bucket, value) <- buckets.zip(aggregateValues)
        } yield AggregateItem(bucket, value)

      } yield Aggregates(fields, values)
    }
}
