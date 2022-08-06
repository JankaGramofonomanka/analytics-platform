package io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic

import scala.collection.mutable.Queue

import scala.util.Try

import cats._
import cats.implicits._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic


class Mock[F[_]: Monad](tags: Queue[UserTag]) {

  private def pure[A](x: A): F[A] = Utils.pure[F, A](x)

  object Publisher extends Topic.Publisher[F, UserTag] {
    def publish(tag: UserTag): F[Unit] = pure(tags.addOne(tag)) >> pure(())
  }

  object Subscriber extends Topic.Subscriber[F, UserTag] {

    private def getTag: Stream[F, Option[UserTag]] = Stream.eval {
      for {

        // TODO make this work for all `F`s or change context bounds to ensure recomputation
        // this forces the next step to recompute in case of `F` being `IO`
        _ <- pure(())
        
        tag <- pure(Try(tags.dequeue()))

      } yield tag.toOption
    }
    
    def subscribe: Stream[F, UserTag] = {
    
      getTag.unNone.repeat
    }
  }

}

object Mock {
  def default[F[_]: Monad] = new Mock[F](new Queue)
}




