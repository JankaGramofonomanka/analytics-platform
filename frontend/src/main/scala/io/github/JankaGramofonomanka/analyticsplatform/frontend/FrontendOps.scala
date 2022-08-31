package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.Sync
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic

class FrontendOps[F[_]: Sync](
  profiles:   KeyValueDB[F, Cookie, Profile],
  aggregates: KeyValueDB[F, AggregateKey, AggregateVB],
  tagsToAggregate: Topic.Publisher[F, UserTag],
) {
  
  def storeTag(tag: UserTag): F[Unit] = tagsToAggregate.publish(tag)

  def getProfile(cookie: Cookie, timeRange: TimeRange, limit: Limit): F[Profile] = for {
    profile <- profiles.get(cookie)
    views = profile.value.views .filter(tag => timeRange.contains(tag.time)).take(limit)
    buys  = profile.value.buys  .filter(tag => timeRange.contains(tag.time)).take(limit)
  } yield Profile(cookie, views, buys)

  def getAggregates(
      timeRange:  TimeRange,
      fields:     AggregateFields,
  ): F[Aggregates] = {
    
      val buckets = timeRange.getBuckets
      val keys = buckets.map(bucket => AggregateKey.fromFields(bucket, fields))
      for {
        aggregateVBs <- keys.traverse { key => aggregates.get(key).map(_.value) }
        
        values = for {
          (bucket, vb) <- buckets.zip(aggregateVBs)
          value = vb.getValue(fields.action)
        } yield AggregateItem(bucket, value)

      } yield Aggregates(fields, values)
    }
}
