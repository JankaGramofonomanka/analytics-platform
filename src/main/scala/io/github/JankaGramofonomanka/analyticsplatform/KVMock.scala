package io.github.JankaGramofonomanka.analyticsplatform

import scala.collection.mutable.Map
import cats.effect.IO
import cats.implicits._

import java.time.{LocalDateTime, ZoneId}
import java.util.Calendar

import org.apache.commons.lang3.time.DateUtils

import io.github.JankaGramofonomanka.analyticsplatform.Data._
import io.github.JankaGramofonomanka.analyticsplatform.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.TagTopic


object KVMock {

  val profiles: Map[Cookie, SimpleProfile] = Map()
  val aggregates: Map[AggregateInfo, AggregateValue] = Map()


  case class AggregateInfo(
    bucket:     Bucket,
    action:     Action,
    origin:     Option[Origin],
    brandId:    Option[BrandId],
    categoryId: Option[CategoryId],
  )
  
  object AggregateInfo {
    private def someAndNone[A](a: A): List[Option[A]] = List(Some(a), None)

    def fromTag(tag: UserTag): List[AggregateInfo] = {
      val bucket = DateUtils.round(tag.time, Calendar.MINUTE)

      for {
        optOrigin     <- someAndNone(tag.origin)
        optBrandId    <- someAndNone(tag.productInfo.brandId)
        optCategoryId <- someAndNone(tag.productInfo.categoryId)
      } yield AggregateInfo(bucket, tag.action, optOrigin, optBrandId, optCategoryId)
    }

  }
  

  object DB extends KeyValueDB[IO] {
    

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
        timeRange,
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
      val dt = LocalDateTime.ofInstant(info.bucket.toInstant, ZoneId.systemDefault())
      (   timeRange.contains(dt)
      &&  info.action     == action
      &&  info.origin     == origin
      &&  info.brandId    == brandId
      &&  info.categoryId == categoryId
      )
    }
  }


  object Topic extends TagTopic[IO] {

    private def getAggregate(info: AggregateInfo): IO[AggregateValue]
      // TODO: replace (0, 0) with a constant or whatever
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

