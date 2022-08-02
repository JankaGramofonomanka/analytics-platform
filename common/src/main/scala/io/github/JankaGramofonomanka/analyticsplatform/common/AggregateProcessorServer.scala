package io.github.JankaGramofonomanka.analyticsplatform.common

import cats.effect.Async
import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.AggregateProcessorOps
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.AggregatesDB



object AggregateProcessorServer {

  def stream[F[_]: Async](
    tagsToAggregate: Topic.Subscriber[F, UserTag],
    aggregates: AggregatesDB[F],
  ): Stream[F, Nothing] = {
    
    val ops = new AggregateProcessorOps[F](tagsToAggregate, aggregates)

    ops.processTags
  }.drain
}
