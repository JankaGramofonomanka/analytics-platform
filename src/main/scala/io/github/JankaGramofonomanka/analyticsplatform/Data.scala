package io.github.JankaGramofonomanka.analyticsplatform

import java.time.{LocalDateTime, ZoneId}
import java.time.temporal.ChronoUnit
import java.util.{Calendar, Date}

import cats.syntax.either._

import org.apache.commons.lang3.time.DateUtils

object Data {
  final case class Cookie(value: String) extends AnyVal

  final case class Timestamp(value: LocalDateTime) extends AnyVal {
    def getBucket: Bucket = {
      val zone = ZoneId.systemDefault
      
      val toRound = Date.from(value.atZone(zone).toInstant)
      val rounded = DateUtils.round(toRound, Calendar.MINUTE)
      
      Bucket(LocalDateTime.ofInstant(rounded.toInstant, zone))
    }
  }
  object Timestamp {
    def parse(s: String): Either[String, Timestamp]
      = Either.catchNonFatal(Timestamp(LocalDateTime.parse(s)))
        .leftMap(err => "Cannot parse datetime: " + err.getMessage)
  }

  final case class Bucket(value: LocalDateTime) extends AnyVal {
    def addMinutes(n: Long): Bucket = Bucket(value.plus(n, ChronoUnit.MINUTES))
    def toTimestamp: Timestamp = Timestamp(value)
  }

  final case class TimeRange(from: Timestamp, to: Timestamp) {
    def contains(datetime: Timestamp): Boolean
      = from.value.isBefore(datetime.value) && datetime.value.isBefore(to.value)

    // TODO change `List` to something else (`Stream`?)
    def getBuckets: List[Bucket] = {

      val numBuckets = ChronoUnit.MINUTES.between(from.value, to.value)

      // TODO round down or up?
      val first = from.getBucket

      // TODO range inclusive or exclusive?
      val range = Range.Long(0, numBuckets, 1)
      range.map(n => first.addMinutes(n)).toList
    }
  }

  object TimeRange {

    def parse(s: String): Either[String, TimeRange] = {
      val items = s.split("_")
      for {

        fromS <- Either.catchNonFatal(items(0)).leftMap(_ => "Cannot parse time range")
        toS   <- Either.catchNonFatal(items(1)).leftMap(_ => "Cannot parse time range")
        
        from  <- Timestamp.parse(fromS)
        to    <- Timestamp.parse(toS)
        
      } yield TimeRange(from, to)
    }
  }

  type Limit = Int

  sealed trait Action
  object VIEW extends Action
  object BUY extends Action

  sealed trait Aggregate
  object COUNT extends Aggregate
  object SUM_PRICE extends Aggregate

  final case class Origin(value: String)      extends AnyVal
  final case class BrandId(value: String)     extends AnyVal
  final case class CategoryId(value: String)  extends AnyVal

  final case class Country(value: String)   extends AnyVal
  final case class Price(value: Int)        extends AnyVal {
    def +(other: Price) = Price(value + other.value)
  }
  final case class ProductId(value: String) extends AnyVal

  
  sealed trait Device
  object PC     extends Device
  object MOBILE extends Device
  object TV     extends Device

  final case class UserTag(
    time:         Timestamp,
    cookie:       Cookie,
    country:      Country,
    device:       Device,
    action:       Action,
    origin:       Origin,
    productInfo:  ProductInfo,
  )

  final case class ProductInfo(
    productId:  ProductId,
    brandId:    BrandId,
    categoryId: CategoryId,
    price:      Price,
  )

  final case class SimpleProfile(tags: Array[UserTag]) {
    def update(tag: UserTag): SimpleProfile = {
      // TODO replace 200 with a constant or whatever
      val newTags = (Array(tag) ++ tags).sortWith((t1, t2) => t1.time.value.isAfter(t2.time.value)).take(200)
      SimpleProfile(newTags)
    }
  }

  object SimpleProfile {
    val empty: SimpleProfile = SimpleProfile(Array())
    val default: SimpleProfile = empty
  }

  final case class PrettyProfile(cookie: Cookie, views: Array[UserTag], buys: Array[UserTag])

  final case class Aggregates(fields: AggregateFields, values: List[(Bucket, AggregateValue)])
  final case class AggregateFields(
    action: Action,
    count: Boolean,
    sumPrice: Boolean,
    origin: Option[Origin],
    brandId: Option[BrandId],
    categoryId: Option[CategoryId],
  )

  final case class AggregateValue(count: Int, sumPrice: Price)
  object AggregateValue {
    val empty: AggregateValue = AggregateValue(0, Price(0))
    val default: AggregateValue = empty
  }


  case class AggregateInfo(
    bucket:     Bucket,
    action:     Action,
    origin:     Option[Origin],
    brandId:    Option[BrandId],
    categoryId: Option[CategoryId],
  )
  
  object AggregateInfo {
    private def someAndNone[A](a: A): List[Option[A]] = List(Some(a), None)

    /*  Returns list of aggregate infos such that their corresponding aggregate 
        values must be updated when `tag` is added to the database
    */
    def fromTag(tag: UserTag): List[AggregateInfo] = {
      val bucket = tag.time.getBucket
      
      for {
        optOrigin     <- someAndNone(tag.origin)
        optBrandId    <- someAndNone(tag.productInfo.brandId)
        optCategoryId <- someAndNone(tag.productInfo.categoryId)
      } yield AggregateInfo(bucket, tag.action, optOrigin, optBrandId, optCategoryId)
    }
  }

  case class AggregateItem(info: AggregateInfo, value: AggregateValue)

}
