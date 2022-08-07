package io.github.JankaGramofonomanka.analyticsplatform.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import cats.syntax.either._

import io.github.JankaGramofonomanka.analyticsplatform.common.ErrorMessages._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config

object Data {
  final case class Cookie(value: String) extends AnyVal

  final case class Timestamp(value: LocalDateTime) extends AnyVal {
    def getBucket: Bucket = {

      val year    = value.getYear
      val month   = value.getMonthValue
      val day     = value.getDayOfMonth
      val hour    = value.getHour
      val minute  = value.getMinute

      Bucket(LocalDateTime.of(year, month, day, hour, minute))
    }
  }
  object Timestamp {
    def parse(s: String): Either[String, Timestamp]
      = Either.catchNonFatal(Timestamp(LocalDateTime.parse(s)))
        .leftMap(err => cannotParseWithMsg("datetime", err.getMessage))

    def encode(ts: Timestamp): String = ts.value.format(formatter)

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
  }

  // TODO shouldn't `value` be private?
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
      = (!from.value.isAfter(datetime.value)) && datetime.value.isBefore(to.value)

    // TODO is `Vector` ok?
    def getBuckets: Vector[Bucket] = {

      val numBuckets = ChronoUnit.MINUTES.between(from.value, to.value)

      val first = from.getBucket

      val range = Range.Long(0, numBuckets, 1)
      range.map(n => first.addMinutes(n)).toVector
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

  final case class SimpleProfile(tags: Vector[UserTag]) extends AnyVal {

    // TODO make `Config.Other.numTagsToKeep` a parameter
    def update(tag: UserTag): SimpleProfile = {
      
      val newTags = insertTagInOrder(tags, tag).take(Config.Other.numTagsToKeep)
      SimpleProfile(newTags)
    }

    def prettify(cookie: Cookie): PrettyProfile = {
      val (views, buys) = tags.partition(_.action == VIEW)
      PrettyProfile(cookie, views, buys)
    }

    private def insertTagInOrder(tags: Vector[UserTag], tag: UserTag): Vector[UserTag] = {
      val (prefix, postfix) = tags.span(_.time.value.isAfter(tag.time.value))
      prefix ++ Vector(tag) ++ postfix
    }
  }

  object SimpleProfile {
    val default: SimpleProfile = SimpleProfile(Vector())
  }

  final case class PrettyProfile(cookie: Cookie, views: Vector[UserTag], buys: Vector[UserTag]) {
    def simplify: SimpleProfile = SimpleProfile(sortTags(views ++ buys))

    private def sortTags(tags: Vector[UserTag]): Vector[UserTag]
      = tags.sortWith((t1, t2) => t1.time.value.isAfter(t2.time.value))
  }

  final case class Aggregates(fields: AggregateFields, items: Vector[AggregateItem])
  final case class AggregateFields(
    action: Action,
    count: Boolean,
    sumPrice: Boolean,
    origin: Option[Origin],
    brandId: Option[BrandId],
    categoryId: Option[CategoryId],
  )

  final case class AggregateItem(bucket: Bucket, value: AggregateValue)

  final case class AggregateValue(count: Int, sumPrice: Price)
  object AggregateValue {
    val default: AggregateValue = AggregateValue(0, Price(0))
  }


  final case class AggregateKey(
    bucket:     Bucket,
    action:     Action,
    origin:     Option[Origin],
    brandId:    Option[BrandId],
    categoryId: Option[CategoryId],
  )
  
  object AggregateKey {
    private def someAndNone[A](a: A): List[Option[A]] = List(Some(a), None)

    /*  Returns list of aggregate keys such that their corresponding aggregate 
        values must be updated when `tag` is added to the database
    */
    def fromTag(tag: UserTag): List[AggregateKey] = {
      val bucket = tag.time.getBucket
      
      for {
        optOrigin     <- someAndNone(tag.origin)
        optBrandId    <- someAndNone(tag.productInfo.brandId)
        optCategoryId <- someAndNone(tag.productInfo.categoryId)
      } yield AggregateKey(bucket, tag.action, optOrigin, optBrandId, optCategoryId)
    }

    def fromFields(bucket: Bucket, fields: AggregateFields): AggregateKey
      = AggregateKey(bucket, fields.action, fields.origin, fields.brandId, fields.categoryId)
  }

  

}
