package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats.effect.Sync
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic

class FrontendOps[F[_]: Sync](
  profiles: ProfilesDB[F],
  aggregates: AggregatesDB[F],
  tagsToAggregate: Topic.Publisher[F, UserTag]
) {

  def storeTag(tag: UserTag): F[Unit] = for {

    profile <- profiles.getProfile(tag.cookie)
    updatedProfile = profile.update(tag)
    _ <- profiles.updateProfile(tag.cookie, updatedProfile)
    _ <- tagsToAggregate.publish(tag)
  } yield ()


  def getProfile(cookie: Cookie, timeRange: TimeRange, limit: Limit): F[PrettyProfile] = for {
    profile <- profiles.getProfile(cookie)
    limited = profile.tags.filter(tag => timeRange.contains(tag.time)).take(limit)
    (views, buys) = limited.partition(_.action == VIEW)
    } yield PrettyProfile(cookie, views, buys)

  def getAggregates(
      timeRange:  TimeRange,
      action:     Action,
      count:      Boolean,
      sumPrice:   Boolean,
      origin:     Option[Origin],
      brandId:    Option[BrandId],
      categoryId: Option[CategoryId],
  ): F[Aggregates]
    = aggregates.getAggregates(timeRange, action, count, sumPrice, origin, brandId, categoryId)
}
