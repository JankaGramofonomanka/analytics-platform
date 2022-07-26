package io.github.JankaGramofonomanka.analyticsplatform

import java.time.LocalDateTime
import java.util.Date

object Data {
  final case class Cookie(value: String) extends AnyVal
  final case class TimeRange(from: LocalDateTime, to: LocalDateTime) {
    def contains(datetime: LocalDateTime): Boolean
      = from.isBefore(datetime) && datetime.isBefore(to)
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
    def update(tag: UserTag): SimpleProfile = ???
  }

  object SimpleProfile {
    val empty: SimpleProfile = SimpleProfile(Array())
  }

  final case class PrettyProfile(cookie: Cookie, views: Array[UserTag], buys: Array[UserTag])

  type Bucket = Date
  final case class Aggregates(fields: AggregateFields, values: List[(Bucket, AggregateValue)])
  final case class AggregateFields(
    timeRange: TimeRange,
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
