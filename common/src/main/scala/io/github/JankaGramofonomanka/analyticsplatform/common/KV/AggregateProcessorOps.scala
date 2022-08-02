package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats.effect.ExitCode
import cats._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic

class AggregateProcessorOps[F[_]](tagsToAggregate: Topic.Subscriber[F, UserTag]) {

  def processTags: Stream[F, ExitCode] = for {
    tag <- tagsToAggregate.subscribe
    c <- implicitly[Monad[Stream[F, *]]].pure {
      println(s"tag: $tag")
      ExitCode.Success
    }
  } yield c
}


