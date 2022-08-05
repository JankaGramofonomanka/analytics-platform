package io.github.JankaGramofonomanka.analyticsplatform.common

import cats.effect.Async
import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.AggregateProcessorOps
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.AggregatesDB



object AggregateProcessorServer {

  def stream[F[_]: Async](
    aggregates:       AggregatesDB[F],
    tagsToAggregate:  Topic.Subscriber[F, UserTag],
  ): Stream[F, Nothing] = {
    
    val ops = new AggregateProcessorOps[F](aggregates, tagsToAggregate)

    ops.processTags
  }.drain
}
