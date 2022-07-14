package io.github.JankaGramofonomanka.analyticsplatform

import io.github.JankaGramofonomanka.analyticsplatform.Data._

abstract class KeyValueDB[F[_]] {

  def getProfile(cookie: Cookie): F[SimpleProfile]
  def updateProfile(cookie: Cookie, profile: SimpleProfile): F[Unit]
  def getAggregates(
      timeRange:  TimeRange,
      action:     Action,
      count:      Boolean,
      sumPrice:   Boolean,
      origin:     Option[Origin],
      brandId:    Option[BrandId],
      categoryId: Option[CategoryId],
  ): F[Aggregates]
}




