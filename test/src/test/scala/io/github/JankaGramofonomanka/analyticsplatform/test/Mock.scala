package io.github.JankaGramofonomanka.analyticsplatform.test

import scala.collection.mutable.{Map, Queue}
import scala.util.Try

import cats.effect._

import fs2.Stream

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic.{Publisher, Subscriber}


object Mock {

  class DB(
    private val profiles:   Map[Cookie, TrackGen[Profile]],
    private val aggregates: Map[AggregateKey, TrackGen[AggregateVB]],
  ) {

    private def tryUpdate[K, V](key: K, value: TrackGen[V], map: Map[K, TrackGen[V]]): IO[Boolean]

      = IO.delay {

        synchronized {
          val newValue = TrackGen(value.value, value.generation + 1)
          val old = map.get(key)
          old match {
            case None => {
                map.put(key, newValue)
                true
              }
            case Some(old) => {
              if (old.generation == value.generation) {
                map.put(key, newValue) 
                true
              } else {
                false
              }
            }
          }
        }
      }

    object Profiles extends KeyValueDB[IO, Cookie, Profile] {
      
      def get(cookie: Cookie): IO[TrackGen[Profile]]
        = IO.delay { profiles.get(cookie).getOrElse(TrackGen.default(Profile.default(cookie))) }

      def update(cookie: Cookie, profile: TrackGen[Profile]): IO[Boolean]
        = tryUpdate(cookie, profile, profiles)

    }
    
    object Aggregates extends KeyValueDB[IO, AggregateKey, AggregateVB] {
      
      def get(key: AggregateKey): IO[TrackGen[AggregateVB]]
        = IO.delay { aggregates.get(key).getOrElse(TrackGen.default(AggregateVB.default)) }
      
      def update(key: AggregateKey, vb: TrackGen[AggregateVB]): IO[Boolean]
        = tryUpdate(key, vb, aggregates)  
    }
  }

  object DB {
    def default = new DB(Map(), Map())
  }

  class Topic(tags: Queue[UserTag]) {

    object Publisher extends Publisher[IO, UserTag] {
      def publish(tag: UserTag): IO[Unit] = IO.delay(tags.addOne(tag)) >> IO.delay(())
    }

    object Subscriber extends Subscriber[IO, UserTag] {

      private def getTag: Stream[IO, Option[UserTag]] = Stream.eval {
        for {

          // this forces the next step to recompute
          _ <- IO.delay(())
          
          tag <- IO.delay(Try(tags.dequeue()))

        } yield tag.toOption
      }
      
      def subscribe: Stream[IO, Seq[UserTag]] = {
      
        getTag.unNone.repeat.map(t => Seq(t))
      }
    }

  }

  object Topic {
    def default = new Topic(new Queue)
  }

}
