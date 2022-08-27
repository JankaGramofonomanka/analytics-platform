package io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor

import cats.effect.Async
import cats.effect.ExitCode
import cats.implicits._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB

class AggregateProcessorOps[F[_]: Async](
  aggregates:       KeyValueDB[F, AggregateKey, AggregateValue],
  tagsToAggregate:  Topic.Subscriber[F, UserTag],
) {

  private def processTag(tag: UserTag): Stream[F, ExitCode] = Stream.eval {
    val keys = AggregateKey.fromTag(tag)

    for {
      _ <- keys.traverse { key => Utils.tryTillSuccess {

          for {
            // TODO It seems that without this line, the next line is computed 
            //      only once, causing an infinite loop
            _ <- Utils.pure[F, Unit](())
            value <- aggregates.get(key)
            newValue = value.map(_.update(tag.productInfo.price))
            
            result <- aggregates.update(key, newValue)
          } yield result
        }

      }
    } yield ExitCode.Success
  }

  def processTags: Stream[F, ExitCode] = for {
    tag <- tagsToAggregate.subscribe
    exitCode <- processTag(tag)
  } yield exitCode
}


