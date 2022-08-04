package io.github.JankaGramofonomanka.analyticsplatform.common

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{Calendar, Date}

import cats.syntax.either._

import org.apache.commons.lang3.time.DateUtils

import io.github.JankaGramofonomanka.analyticsplatform.common.ErrorMessages._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config

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
        .leftMap(err => cannotParseWithMsg("datetime", err.getMessage))

    def encode(ts: Timestamp): String = ts.value.format(formatter)

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
  }

  final case class Bucket(value: LocalDateTime) extends AnyVal {
    def addMinutes(n: Long): Bucket = Bucket(value.plus(n, ChronoUnit.MINUTES))
    def toTimestamp: Timestamp = Timestamp(value)
  }

  object Bucket {
    def parse(s: String): Either[String, Bucket]
      = Timestamp.parse(s).map(_.getBucket)

    def encode(b: Bucket): String = b.value.format(formatter)

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
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

        fromS <- Either.catchNonFatal(items(0)).leftMap(_ => cannotParse("time range"))
        toS   <- Either.catchNonFatal(items(1)).leftMap(_ => cannotParse("time range"))
        
        from  <- Timestamp.parse(fromS)
        to    <- Timestamp.parse(toS)
        
      } yield TimeRange(from, to)
    }

    def encode(tr: TimeRange): String = {
      val fromS = Timestamp.encode(tr.from)
      val toS   = Timestamp.encode(tr.to)
      s"${fromS}_${toS}"}
  }

  type Limit = Int

  sealed trait Action
  object VIEW extends Action
  object BUY extends Action

  object Action {

    def parse(s: String): Either[String, Action] = s match {
      case "VIEW" => Right(VIEW)
      case "BUY"  => Right(BUY)
      case s      => Left(unknown("action", s))
    }

    def encode(action: Action): String = action match {
      case VIEW => "VIEW"
      case BUY  => "BUY"
    }

  }

  sealed trait Aggregate
  object COUNT extends Aggregate
  object SUM_PRICE extends Aggregate

  object Aggregate {
    
    def parse(s: String): Either[String, Aggregate] = s match { 
      case "COUNT"      => Right(COUNT)
      case "SUM_PRICE"  => Right(SUM_PRICE)
      case s            => Left(unknown("aggregate", s))
    }

    def encode(aggregate: Aggregate): String = aggregate match {
      case COUNT      => "COUNT"
      case SUM_PRICE  => "SUM_PRICE"
    }

  }

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

  object Device {
    
    def parse(s: String): Either[String, Device] = s match {
      case "PC"     => Right(PC)
      case "MOBILE" => Right(MOBILE)
      case "TV"     => Right(TV)
      case s        => Left(unknown("device", s))
    }
    
    def encode(device: Device): String = device match {
      case PC     => "PC"
      case MOBILE => "MOBILE"
      case TV     => "TV"
    }

  }

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

  // TODO replace array with sometyhing else so that comparing idenctical profiles returns `true`
  final case class SimpleProfile(tags: Array[UserTag]) extends AnyVal {
    def update(tag: UserTag): SimpleProfile = {
      // TODO figure out efficient sorting
      val newTags = (Array(tag) ++ tags)
                      .sortWith((t1, t2) => t1.time.value.isAfter(t2.time.value))
                      .take(Config.Other.numTagsToKeep)
      SimpleProfile(newTags)
    }
  }

  object SimpleProfile {
    val default: SimpleProfile = SimpleProfile(Array())
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
    val default: AggregateValue = AggregateValue(0, Price(0))
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
