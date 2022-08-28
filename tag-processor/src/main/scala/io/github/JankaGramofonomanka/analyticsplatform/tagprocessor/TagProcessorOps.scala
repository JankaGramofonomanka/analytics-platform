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

  private def mkProfiles(tags: Seq[UserTag]): Map[Cookie, SimpleProfile] = {
    val processTag = (map: Map[Cookie, SimpleProfile], tag: UserTag)
          => Utils.updateMap[Cookie, SimpleProfile](_.addOne(tag))(map)(tag.cookie, SimpleProfile(Vector(tag)))
    tags.foldLeft[Map[Cookie, SimpleProfile]](Map())(processTag)
  }

  private def mkProfilesStream(tags: Seq[UserTag]): Stream[F, (Cookie, SimpleProfile)]
    = Stream.evalSeq {
      Utils.pure[F, Seq[(Cookie, SimpleProfile)]](mkProfiles(tags).toSeq)
    }

  private def mkAggregates(tags: Seq[UserTag]): Map[AggregateKey, AggregateValue] = {
    
    val processTag = (map: Map[AggregateKey, AggregateValue], tag: UserTag) => {
      val keys = AggregateKey.fromTag(tag)
      val value = AggregateValue.fromTag(tag)
      val updateKey = (map: Map[AggregateKey, AggregateValue], key: AggregateKey)
            => Utils.updateMap[AggregateKey, AggregateValue](_ + value)(map)(key, value)
      
      keys.foldLeft[Map[AggregateKey, AggregateValue]](map)(updateKey)
    }

    tags.foldLeft[Map[AggregateKey, AggregateValue]](Map())(processTag)
  }

  def mkAggregatesStream(tags: Seq[UserTag]): Stream[F, (AggregateKey, AggregateValue)]
    = Stream.evalSeq {
      Utils.pure[F, Seq[(AggregateKey, AggregateValue)]](mkAggregates(tags).toSeq)
    }

  private def tryUpdateProfile(cookie: Cookie, profile: SimpleProfile): F[Boolean] = for {
    _ <- Utils.pure[F, Unit](())
    oldProfile <- profiles.get(cookie)
    newProfile = oldProfile.map(_ ++ profile).map(_.limit(env.NUM_TAGS_TO_KEEP))
    result <- profiles.update(cookie, newProfile)

  } yield result

  private def tryUpdateAggregate(key: AggregateKey, value: AggregateValue): F[Boolean] = for {
    _ <- Utils.pure[F, Unit](())
    oldValue <- aggregates.get(key)
    newValue = oldValue.map(_ + value)
    result <- aggregates.update(key, newValue)
  } yield result

  private val processProfile: ((Cookie, SimpleProfile)) => F[Unit]
    = Utils.uncurry {
    (cookie, profile) => Utils.tryTillSuccess(tryUpdateProfile(cookie, profile))
  }

  private val processAggregate: ((AggregateKey, AggregateValue)) => F[Unit]
    = Utils.uncurry {
      (key, value) => Utils.tryTillSuccess(tryUpdateAggregate(key, value))
    }

  private def processProfiles(tags: Seq[UserTag]): Stream[F, Unit]
    = mkProfilesStream(tags).parEvalMap(env.MAX_PARALLEL_WRITES / 2)(processProfile)

  private def processAggregates(tags: Seq[UserTag]): Stream[F, Unit]
    = mkAggregatesStream(tags).parEvalMap(env.MAX_PARALLEL_WRITES / 2)(processAggregate)

  def processTags: Stream[F, ExitCode] = for {
    tags <- tagsToAggregate.subscribe
    
    _ <- processProfiles(tags).concurrently(processAggregates(tags))

  } yield ExitCode.Success
}


