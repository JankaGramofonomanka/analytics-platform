package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats._
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

trait AggregatesDB[F[_]] {

  def getAggregate(info: AggregateInfo): F[AggregateValue]

  def updateAggregate(info: AggregateInfo, value: AggregateValue): F[Unit]

  def getAggregates(
      timeRange:  TimeRange,
      action:     Action,
      count:      Boolean,
      sumPrice:   Boolean,
      origin:     Option[Origin],
      brandId:    Option[BrandId],
      categoryId: Option[CategoryId],
  )(implicit applicativeF: Applicative[F]): F[Aggregates] = {
    
    val buckets = timeRange.getBuckets
    val infos = buckets.map(bucket => AggregateInfo(bucket, action, origin, brandId, categoryId))
    val fields = AggregateFields(action, count, sumPrice, origin, brandId, categoryId)
    for {
      
      aggregateValues <- infos.traverse { info => getAggregate(info) }
      values = buckets.zip(aggregateValues)
      
    } yield Aggregates(fields, values)
  }
}




