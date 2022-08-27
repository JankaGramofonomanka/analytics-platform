package io.github.JankaGramofonomanka.analyticsplatform.tagprocessor

import cats.effect.Async
import cats.effect.ExitCode
import cats.implicits._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.Config.Environment

class TagProcessorOps[F[_]: Async](
  profiles:         KeyValueDB[F, Cookie, SimpleProfile],
  aggregates:       KeyValueDB[F, AggregateKey, AggregateValue],
  tagsToAggregate:  Topic.Subscriber[F, UserTag],
)(implicit env: Environment) {

  private def tryStoreTag(tag: UserTag): F[Boolean] = for {
    profile <- profiles.get(tag.cookie)
    updated = profile.map(_.update(tag, env.NUM_TAGS_TO_KEEP))
    result <- profiles.update(tag.cookie, updated)
  } yield result

  private def processTag(tag: UserTag): Stream[F, ExitCode] = Stream.eval {
    val keys = AggregateKey.fromTag(tag)

    for {
      _ <- Utils.tryTillSuccess(tryStoreTag(tag))

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


