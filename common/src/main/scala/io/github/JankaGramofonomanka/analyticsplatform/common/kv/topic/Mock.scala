package io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic

import scala.collection.mutable.Queue

import scala.util.Try

import cats.effect._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic


class Mock(tags: Queue[UserTag]) {

  object Publisher extends Topic.Publisher[IO, UserTag] {
    def publish(tag: UserTag): IO[Unit] = IO.delay(tags.addOne(tag)) >> IO.delay(())
  }

  object Subscriber extends Topic.Subscriber[IO, UserTag] {

    private def getTag: Stream[IO, Option[UserTag]] = Stream.eval {
      for {

        // this forces the next step to recompute
        _ <- IO.delay(())
        
        tag <- IO.delay(Try(tags.dequeue()))

      } yield tag.toOption
    }
    
    def subscribe: Stream[IO, UserTag] = {
    
      getTag.unNone.repeat
    }
  }

}

object Mock {
  def default = new Mock(new Queue)
}



