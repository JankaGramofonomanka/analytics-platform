package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import scala.collection.mutable.Map

import cats.effect._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.KeyValueDB

class Mock(
  private val profiles:   Map[Cookie, TrackGen[SimpleProfile]],
  private val aggregates: Map[AggregateKey, TrackGen[AggregateValue]],
) {

  private def tryUpdate[K, V](key: K, value: TrackGen[V], map: Map[K, TrackGen[V]]): IO[Boolean]

    // TODO does this guarantee atomic execution?
    = IO.delay {
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

  object Profiles extends KeyValueDB[IO, Cookie, SimpleProfile] {
    
    def get(cookie: Cookie): IO[TrackGen[SimpleProfile]]
      = IO.delay { profiles.get(cookie).getOrElse(TrackGen.default(SimpleProfile.default)) }

    def update(cookie: Cookie, profile: TrackGen[SimpleProfile]): IO[Boolean]
      = tryUpdate(cookie, profile, profiles)

  }
  
  object Aggregates extends KeyValueDB[IO, AggregateKey, AggregateValue] {
    
    def get(key: AggregateKey): IO[TrackGen[AggregateValue]]
      = IO.delay { aggregates.get(key).getOrElse(TrackGen.default(AggregateValue.default)) }
    
    def update(key: AggregateKey, value: TrackGen[AggregateValue]): IO[Boolean]
      = tryUpdate(key, value, aggregates)  
  }
}

object Mock {
  def default = new Mock(Map(), Map())
}
