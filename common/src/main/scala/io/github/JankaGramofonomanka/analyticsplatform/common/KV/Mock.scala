package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import scala.collection.mutable.Map
import cats.effect.IO
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.TagTopic


object Mock {

  val profiles: Map[Cookie, SimpleProfile] = Map()
  val aggregates: Map[AggregateInfo, AggregateValue] = Map()

  

  object DB extends ProfilesDB[IO] with AggregatesDB[IO] {
    

    def getProfile(cookie: Cookie): IO[SimpleProfile]
      = IO.delay(profiles.get(cookie).getOrElse(SimpleProfile.empty))

    def updateProfile(cookie: Cookie, profile: SimpleProfile): IO[Unit]
      = IO.delay(profiles.addOne((cookie, profile)))
    
    def getAggregates(
        timeRange:  TimeRange,
        action:     Action,
        count:      Boolean,
        sumPrice:   Boolean,
        origin:     Option[Origin],
        brandId:    Option[BrandId],
        categoryId: Option[CategoryId],
    ): IO[Aggregates] = IO.delay({

      val filterFunc = filterAggregateInfo(timeRange, action, origin, brandId, categoryId)
      val m = aggregates.filter(t => filterFunc(t._1))
      val values = m.toList.map(t => (t._1.bucket, t._2))
      val fields = AggregateFields(
        action,
        count,
        sumPrice,
        origin,
        brandId,
        categoryId,
      )

      Aggregates(fields, values)
    })

    private def filterAggregateInfo(
      timeRange:  TimeRange,
      action:     Action,
      origin:     Option[Origin],
      brandId:    Option[BrandId],
      categoryId: Option[CategoryId],
    ): AggregateInfo => Boolean = info => {
      (   timeRange.contains(info.bucket.toTimestamp)
      &&  info.action     == action
      &&  info.origin     == origin
      &&  info.brandId    == brandId
      &&  info.categoryId == categoryId
      )
    }
  }


  object Topic extends TagTopic[IO] {

    private def getAggregate(info: AggregateInfo): IO[AggregateValue]
      = IO.delay(aggregates.get(info).getOrElse(AggregateValue.empty))
    
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

