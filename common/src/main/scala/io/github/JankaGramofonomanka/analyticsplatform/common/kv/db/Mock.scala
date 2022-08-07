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
  aggregates: Map[AggregateKey, AggregateValue],
) extends ProfilesDB[F] with AggregatesDB[F] {

  private def pure[A](x: A): F[A] = Utils.pure[F, A](x)
  

  def getProfile(cookie: Cookie): F[SimpleProfile]
    = pure(profiles.get(cookie).getOrElse(SimpleProfile.default))

  def updateProfile(cookie: Cookie, profile: SimpleProfile): F[Unit]
    = pure(profiles.addOne((cookie, profile)))

  def getAggregate(key: AggregateKey): F[AggregateValue]
    = pure(aggregates.get(key).getOrElse(AggregateValue.default))
  
  def updateAggregate(key: AggregateKey, value: AggregateValue): F[Unit]
    = pure(aggregates.put(key, value)) >> pure(())



  object Aggregator extends Topic.Publisher[F, UserTag] {

    private def getAggregate(key: AggregateKey): F[AggregateValue]
      = pure(aggregates.get(key).getOrElse(AggregateValue.default))
    
    private def getAggregateKV(key: AggregateKey): F[(AggregateKey, AggregateValue)] = for {
      v <- getAggregate(key)
    } yield (key -> v)

    def publish(tag: UserTag): F[Unit] = {
      val keys = AggregateKey.fromTag(tag)
      for {
        toUpdate <- keys.traverse(key => getAggregateKV(key))
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
