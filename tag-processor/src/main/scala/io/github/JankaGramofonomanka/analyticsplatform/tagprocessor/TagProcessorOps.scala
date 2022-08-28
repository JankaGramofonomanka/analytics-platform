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
  aggregates:       KeyValueDB[F, AggregateKey, AggregateVB],
  tagsToAggregate:  Topic.Subscriber[F, UserTag],
)(implicit env: Environment) {

  private def mkProfiles(tags: Seq[UserTag]): Map[Cookie, SimpleProfile] = {
    val processTag = (map: Map[Cookie, SimpleProfile], tag: UserTag) => {
      Utils.updateMap[Cookie, SimpleProfile](_.addOne(tag))(map)(tag.cookie, SimpleProfile(Vector(tag)))
    }
    tags.foldLeft[Map[Cookie, SimpleProfile]](Map())(processTag)
  }

  private def mkProfilesStream(tags: Seq[UserTag]): Stream[F, (Cookie, SimpleProfile)]
    = Stream.evalSeq {
      Utils.pure[F, Seq[(Cookie, SimpleProfile)]](mkProfiles(tags).toSeq)
    }

  private def mkAggregates(tags: Seq[UserTag]): Map[AggregateKey, AggregateVB] = {
    
    val processTag = (map: Map[AggregateKey, AggregateVB], tag: UserTag) => {
      val keys = AggregateKey.fromTag(tag)
      val vb = AggregateVB.fromTag(tag)
      val updateKey = (map: Map[AggregateKey, AggregateVB], key: AggregateKey) => {
        Utils.updateMap[AggregateKey, AggregateVB](_ + vb)(map)(key, vb)
      }
      
      keys.foldLeft[Map[AggregateKey, AggregateVB]](map)(updateKey)
    }

    tags.foldLeft[Map[AggregateKey, AggregateVB]](Map())(processTag)
  }

  def mkAggregatesStream(tags: Seq[UserTag]): Stream[F, (AggregateKey, AggregateVB)]
    = Stream.evalSeq {
      Utils.pure[F, Seq[(AggregateKey, AggregateVB)]](mkAggregates(tags).toSeq)
    }

  private def tryUpdateProfile(cookie: Cookie, profile: SimpleProfile): F[Boolean] = for {
    _ <- Utils.pure[F, Unit](())
    oldProfile <- profiles.get(cookie)
    newProfile = oldProfile.map(_ ++ profile).map(_.limit(env.NUM_TAGS_TO_KEEP))
    result <- profiles.update(cookie, newProfile)

  } yield result

  private def tryUpdateAggregate(key: AggregateKey, vb: AggregateVB): F[Boolean] = for {
    _ <- Utils.pure[F, Unit](())
    oldVB <- aggregates.get(key)
    newVB = oldVB.map(_ + vb)
    result <- aggregates.update(key, newVB)
  } yield result

  private val processProfile: ((Cookie, SimpleProfile)) => F[Unit]
    = Utils.uncurry {
      (cookie, profile) => Utils.tryTillSuccess(tryUpdateProfile(cookie, profile))
    }

  private val processAggregate: ((AggregateKey, AggregateVB)) => F[Unit]
    = Utils.uncurry {
      (key, vb) => Utils.tryTillSuccess(tryUpdateAggregate(key, vb))
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


