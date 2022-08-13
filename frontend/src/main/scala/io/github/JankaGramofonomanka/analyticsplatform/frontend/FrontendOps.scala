package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.Sync
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Config.Environment

class FrontendOps[F[_]: Sync](
  profiles:   KeyValueDB[F, Cookie, SimpleProfile],
  aggregates: KeyValueDB[F, AggregateKey, AggregateValue],
  tagsToAggregate: Topic.Publisher[F, UserTag],
)(implicit env: Environment) {
  
  private def tryStoreTag(tag: UserTag): F[Boolean] = for {
    profile <- profiles.get(tag.cookie)
    updated = profile.map(_.update(tag, env.NUM_TAGS_TO_KEEP))
    result <- profiles.update(tag.cookie, updated)
  } yield result

  def storeTag(tag: UserTag): F[Unit] = for {
    _ <- Utils.tryTillSuccess(tryStoreTag(tag))
    _ <- tagsToAggregate.publish(tag)
  } yield ()


  def getProfile(cookie: Cookie, timeRange: TimeRange, limit: Limit): F[PrettyProfile] = for {
    profile <- profiles.get(cookie)
    tags = profile.value.tags.filter(tag => timeRange.contains(tag.time)).take(limit)
  } yield SimpleProfile(tags).prettify(cookie)

  def getAggregates(
      timeRange:  TimeRange,
      fields:     AggregateFields,
  ): F[Aggregates] = {
    
      val buckets = timeRange.getBuckets
      val keys = buckets.map(bucket => AggregateKey.fromFields(bucket, fields))
      for {
        aggregateValues <- keys.traverse { key => aggregates.get(key).map(_.value) }
        
        values = for {
          (bucket, value) <- buckets.zip(aggregateValues)
        } yield AggregateItem(bucket, value)

      } yield Aggregates(fields, values)
    }
}
