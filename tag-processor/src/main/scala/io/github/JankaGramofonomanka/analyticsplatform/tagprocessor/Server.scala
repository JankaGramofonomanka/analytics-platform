package io.github.JankaGramofonomanka.analyticsplatform.tagprocessor

import cats.effect.Async
import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.TagProcessorOps
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.Config.Environment


object Server {

  def stream[F[_]: Async](
    profiles:         KeyValueDB[F, Cookie, SimpleProfile],
    aggregates:       KeyValueDB[F, AggregateKey, AggregateValue],
    tagsToAggregate:  Topic.Subscriber[F, UserTag],
  )(implicit env: Environment): Stream[F, Nothing] = {
    
    val ops = new TagProcessorOps[F](profiles, aggregates, tagsToAggregate)

    ops.processTags
  }.drain
}
