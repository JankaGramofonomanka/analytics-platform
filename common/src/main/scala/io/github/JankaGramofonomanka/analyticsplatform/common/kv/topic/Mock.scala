package io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic

import scala.collection.mutable.Queue

import cats._
import cats.implicits._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic


class Mock[F[_]: Monad] {

  private val tagsToAggregate: Queue[UserTag] = new Queue

  private def pure[A](x: A): F[A] = Utils.pure[F, A](x)

  object Publisher extends Topic.Publisher[F, UserTag] {
    def publish(tag: UserTag): F[Unit] = pure(tagsToAggregate.addOne(tag)) >> pure(())
  }

  object Subscriber extends Topic.Subscriber[F, UserTag] {
    def subscribe: Stream[F, UserTag] = Stream.eval(pure(tagsToAggregate.dequeue())).repeat
  }

}




