package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import cats.effect.Sync
import cats.effect.ExitCode
import cats.implicits._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.AggregatesDB

class AggregateProcessorOps[F[_]: Sync](
  aggregates:       AggregatesDB[F],
  tagsToAggregate:  Topic.Subscriber[F, UserTag],
) {

  private def processTag(tag: UserTag): Stream[F, ExitCode] = Stream.eval {
    val infos = AggregateInfo.fromTag(tag)

    for {
      _ <- infos.traverse { info =>
        for {
          value <- aggregates.getAggregate(info)
          newValue = AggregateValue(value.count + 1, value.sumPrice + tag.productInfo.price)
          unit <- aggregates.updateAggregate(info, newValue)
        } yield unit
      }
    } yield ExitCode.Success
    
  }

  def processTags: Stream[F, ExitCode] = for {
    tag <- tagsToAggregate.subscribe
    exitCode <- processTag(tag)
  } yield exitCode
}


