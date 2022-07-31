package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

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




