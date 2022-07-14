package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.Sync
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.Data._
import io.github.JankaGramofonomanka.analyticsplatform.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.TagTopic

class KVFrontend[F[_]: Sync](db: KeyValueDB[F], tagTopic: TagTopic[F]) {

  def storeTag(tag: UserTag): F[Unit] = for {

    profile <- db.getProfile(tag.cookie)
    updatedProfile = profile.update(tag)
    _ <- db.updateProfile(tag.cookie, updatedProfile)
    _ <- tagTopic.publish(tag)
  } yield ()


  def getProfile(cookie: Cookie, timeRange: TimeRange, limit: Limit): F[PrettyProfile] = for {
    profile <- db.getProfile(cookie)
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
    = db.getAggregates(timeRange, action, count, sumPrice, origin, brandId, categoryId)
}
