package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats.effect.ExitCode
import cats._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic

class AggregateProcessorOps[F[_]](tagsToAggregate: Topic.Subscriber[F, UserTag]) {

  private def processTag(tag: UserTag): Stream[F, ExitCode] = {
    implicitly[Monad[Stream[F, *]]].pure {
      println(s"tag: $tag")
      ExitCode.Success
    }
  }

  def processTags: Stream[F, ExitCode] = for {
    tag <- tagsToAggregate.subscribe
    exitCode <- processTag(tag)
  } yield exitCode
}


