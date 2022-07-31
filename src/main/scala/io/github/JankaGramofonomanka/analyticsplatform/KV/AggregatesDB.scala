package io.github.JankaGramofonomanka.analyticsplatform.KV

import io.github.JankaGramofonomanka.analyticsplatform.Data._

trait AggregatesDB[F[_]] {

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




