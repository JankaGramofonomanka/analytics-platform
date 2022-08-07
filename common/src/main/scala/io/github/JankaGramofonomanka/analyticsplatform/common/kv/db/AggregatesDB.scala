package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

trait AggregatesDB[F[_]] {

  def getAggregate(info: AggregateInfo): F[AggregateValue]
  def updateAggregate(info: AggregateInfo, value: AggregateValue): F[Unit]

}




