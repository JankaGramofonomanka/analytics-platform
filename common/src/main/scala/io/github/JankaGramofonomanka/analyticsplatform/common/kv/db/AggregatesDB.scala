package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

trait AggregatesDB[F[_]] {

  def getAggregate(key: AggregateKey): F[TrackGen[AggregateValue]]
  def updateAggregate(key: AggregateKey, value: TrackGen[AggregateValue]): F[Boolean]

}




