package io.github.JankaGramofonomanka.analyticsplatform

import java.time.LocalDateTime

import cats.syntax.either._

object Data {
  final case class Cookie(value: String) extends AnyVal

  def parseLocalDateTime(s: String): Either[String, LocalDateTime]
    = Either.catchNonFatal(LocalDateTime.parse(s)).leftMap(err => "Cannot parse datetime: " + err.getMessage)

  final case class TimeRange(from: LocalDateTime, to: LocalDateTime) {
    def contains(datetime: LocalDateTime): Boolean
      = from.isBefore(datetime) && datetime.isBefore(to)
  }

  def parseTimeRange(s: String): Either[String, TimeRange] = {
      val items = s.split("_")
      for {

        fromS <- Either.catchNonFatal(items(0)).leftMap(_ => "Cannot parse time range")
        toS   <- Either.catchNonFatal(items(1)).leftMap(_ => "Cannot parse time range")
        
        from  <- parseLocalDateTime(fromS)
        to    <- parseLocalDateTime(toS)
        
      } yield TimeRange(from, to)
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
    time:         LocalDateTime,
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
      val newTags = (Array(tag) ++ tags).sortWith((t1, t2) => t1.time.isAfter(t2.time)).take(200)
      SimpleProfile(newTags)
    }
  }

  object SimpleProfile {
    val empty: SimpleProfile = SimpleProfile(Array())
  }

  final case class PrettyProfile(cookie: Cookie, views: Array[UserTag], buys: Array[UserTag])

  type Bucket = LocalDateTime
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
  }
}
