package io.github.JankaGramofonomanka.analyticsplatform.common

import fs2.Stream
import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.AggregateProcessorOps
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic



object AggregateProcessorServer {

  def stream[F[_]](tagsToAggregate: Topic.Subscriber[F, UserTag]): Stream[F, Nothing] = {
    
    val ops = new AggregateProcessorOps[F](tagsToAggregate)

    ops.processTags
  }.drain
}
