package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import scala.collection.mutable.Map

import cats._
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic


class Mock[F[_]: Monad](
  profiles:   Map[Cookie, SimpleProfile],
  aggregates: Map[AggregateInfo, AggregateValue],
) extends ProfilesDB[F] with AggregatesDB[F] {

  private def pure[A](x: A): F[A] = Utils.pure[F, A](x)
  

  def getProfile(cookie: Cookie): F[SimpleProfile]
    = pure(profiles.get(cookie).getOrElse(SimpleProfile.default))

  def updateProfile(cookie: Cookie, profile: SimpleProfile): F[Unit]
    = pure(profiles.addOne((cookie, profile)))

  def getAggregate(info: AggregateInfo): F[AggregateValue]
    = pure(aggregates.get(info).getOrElse(AggregateValue.default))
  
  def updateAggregate(info: AggregateInfo, value: AggregateValue): F[Unit]
    = pure(aggregates.put(info, value)) >> pure(())



  object Aggregator extends Topic.Publisher[F, UserTag] {

    private def getAggregate(info: AggregateInfo): F[AggregateValue]
      = pure(aggregates.get(info).getOrElse(AggregateValue.default))
    
    private def getAggregateKV(info: AggregateInfo): F[(AggregateInfo, AggregateValue)] = for {
      v <- getAggregate(info)
    } yield (info -> v)

    def publish(tag: UserTag): F[Unit] = {
      val aggregateInfos = AggregateInfo.fromTag(tag)
      for {
        toUpdate <- aggregateInfos.traverse(info => getAggregateKV(info))
        toAdd = toUpdate.map { case (k, v)
          => (k -> AggregateValue(v.count + 1, v.sumPrice + tag.productInfo.price))
        }

        _ <- pure(aggregates ++= toAdd)
      } yield ()
    }
  }
}

object Mock {
  def default[F[_]: Monad] = new Mock[F](Map(), Map())
}
