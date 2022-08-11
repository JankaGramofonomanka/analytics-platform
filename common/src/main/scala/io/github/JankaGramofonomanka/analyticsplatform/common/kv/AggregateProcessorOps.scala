package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import cats.effect.Sync
import cats.effect.ExitCode
import cats.implicits._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.AggregatesDB

class AggregateProcessorOps[F[_]: Sync](
  aggregates:       AggregatesDB[F],
  tagsToAggregate:  Topic.Subscriber[F, UserTag],
) {

  private def processTag(tag: UserTag): Stream[F, ExitCode] = Stream.eval {
    val keys = AggregateKey.fromTag(tag)

    for {
      _ <- keys.traverse { key =>
        for {
          value <- aggregates.getAggregate(key)
          
          newValue = value.map(_.update(tag.productInfo.price))
          unit <- Utils.tryTillSuccess(aggregates.updateAggregate(key, newValue))
        } yield unit
      }
    } yield ExitCode.Success
    
  }

  def processTags: Stream[F, ExitCode] = for {
    tag <- tagsToAggregate.subscribe
    exitCode <- processTag(tag)
  } yield exitCode
}


