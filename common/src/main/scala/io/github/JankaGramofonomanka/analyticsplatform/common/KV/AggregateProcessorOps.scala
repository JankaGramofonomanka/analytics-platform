package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats.effect.Sync
import cats.effect.ExitCode
import cats.implicits._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.AggregatesDB

class AggregateProcessorOps[F[_]: Sync](
  tagsToAggregate: Topic.Subscriber[F, UserTag],
  aggregates: AggregatesDB[F],
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


