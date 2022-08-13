package io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor

import cats.effect.Async
import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.AggregateProcessorOps



object Server {

  def stream[F[_]: Async](
    aggregates:       KeyValueDB[F, AggregateKey, AggregateValue],
    tagsToAggregate:  Topic.Subscriber[F, UserTag],
  ): Stream[F, Nothing] = {
    
    val ops = new AggregateProcessorOps[F](aggregates, tagsToAggregate)

    ops.processTags
  }.drain
}
