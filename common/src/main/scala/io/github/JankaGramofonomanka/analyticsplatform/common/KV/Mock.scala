package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import scala.collection.mutable.Map
import cats.effect.IO
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic


object Mock {

  val profiles: Map[Cookie, SimpleProfile] = Map()
  val aggregates: Map[AggregateInfo, AggregateValue] = Map()

  

  object DB extends ProfilesDB[IO] with AggregatesDB[IO] {
    

    def getProfile(cookie: Cookie): IO[SimpleProfile]
      = IO.delay(profiles.get(cookie).getOrElse(SimpleProfile.default))

    def updateProfile(cookie: Cookie, profile: SimpleProfile): IO[Unit]
      = IO.delay(profiles.addOne((cookie, profile)))

    def getAggregate(info: AggregateInfo): IO[AggregateValue]
      = IO.delay(aggregates.get(info).getOrElse(AggregateValue.default))
    
  }


  object TagsToAggregate extends Topic.Publisher[IO, UserTag] {

    private def getAggregate(info: AggregateInfo): IO[AggregateValue]
      = IO.delay(aggregates.get(info).getOrElse(AggregateValue.default))
    
    private def getAggregateKV(info: AggregateInfo): IO[(AggregateInfo, AggregateValue)] = for {
      v <- getAggregate(info)
    } yield (info -> v)

    def publish(tag: UserTag): IO[Unit] = {
      val aggregateInfos = AggregateInfo.fromTag(tag)
      for {
        toUpdate <- aggregateInfos.traverse(info => getAggregateKV(info))
        toAdd = toUpdate.map { case (k, v)
          => (k -> AggregateValue(v.count + 1, v.sumPrice + tag.productInfo.price))
        }

        _ <- IO.delay(aggregates ++= toAdd)
      } yield ()
    }
  }


}

