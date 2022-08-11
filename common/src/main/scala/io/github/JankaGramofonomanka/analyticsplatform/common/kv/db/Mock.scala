package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import scala.collection.mutable.Map

import cats.effect._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.{ProfilesDB, AggregatesDB}

class Mock(
  private val profiles:   Map[Cookie, TrackGen[SimpleProfile]],
  private val aggregates: Map[AggregateKey, TrackGen[AggregateValue]],
) extends ProfilesDB[IO] with AggregatesDB[IO] {

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

  def getProfile(cookie: Cookie): IO[TrackGen[SimpleProfile]]
    = IO.delay { profiles.get(cookie).getOrElse(TrackGen.default(SimpleProfile.default)) }

  def updateProfile(cookie: Cookie, profile: TrackGen[SimpleProfile]): IO[Boolean]
    = tryUpdate(cookie, profile, profiles)

  def getAggregate(key: AggregateKey): IO[TrackGen[AggregateValue]]
    = IO.delay { aggregates.get(key).getOrElse(TrackGen.default(AggregateValue.default)) }
  
  def updateAggregate(key: AggregateKey, value: TrackGen[AggregateValue]): IO[Boolean]
    = tryUpdate(key, value, aggregates)
}

object Mock {
  def default = new Mock(Map(), Map())
}
