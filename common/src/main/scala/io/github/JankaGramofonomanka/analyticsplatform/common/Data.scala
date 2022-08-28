package io.github.JankaGramofonomanka.analyticsplatform.common

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import cats._
import cats.syntax.functor._
import cats.syntax.either._
import cats.syntax.flatMap._

import io.github.JankaGramofonomanka.analyticsplatform.common.ErrorMessages._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils

object Data {

  private object DateFormat {
    val seconds     = "yyyy-MM-dd'T'HH:mm:ss"
    val millis      = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    val secondsZ    = s"$seconds'Z'"
    val millisZ     = s"$millis'Z'"
    val optMillisZ  = s"$seconds[.SSS]'Z'"
  }

  final case class Cookie(value: String) extends AnyVal

  final case class Timestamp(value: LocalDateTime) extends AnyVal {
    def getBucket: Bucket = Bucket(Utils.roundToMinutes(value))

    def isAfter (ts: Timestamp): Boolean = value.isAfter  (ts.value)
    def isBefore(ts: Timestamp): Boolean = value.isBefore (ts.value)
    def isEqual (ts: Timestamp): Boolean = value.isEqual  (ts.value)

  }
  object Timestamp {
    def parse(s: String): Either[String, Timestamp]
      = Either.catchNonFatal(Timestamp(LocalDateTime.parse(s, parseFormatter)))
        .leftMap(err => cannotParseWithMsg("timestamp", err.getMessage))

    def encode(ts: Timestamp): String = ts.value.format(printFormatter)

    private val printFormatter = DateTimeFormatter.ofPattern(DateFormat.millisZ)
    private val parseFormatter = DateTimeFormatter.ofPattern(DateFormat.optMillisZ)
  }

  // TODO how to enforce rounding of `value` on bucket creation?
  final case class Bucket(private val value: LocalDateTime) extends AnyVal {
    def addMinutes(n: Long): Bucket = Bucket(value.plus(n, ChronoUnit.MINUTES))
    
    def getSeconds: Long = value.toEpochSecond(UTC)
    
    def toTimestamp: Timestamp = Timestamp(value)
    def toDateTime: DateTime = value

    def isAfter (bucket: Bucket): Boolean = value.isAfter (bucket.value)
    def isBefore(bucket: Bucket): Boolean = value.isBefore(bucket.value)
    def isEqual (bucket: Bucket): Boolean = value.isEqual (bucket.value)
  }

  object Bucket {
    
    def parse(s: String): Either[String, Bucket]
      = Either.catchNonFatal(Bucket(LocalDateTime.parse(s, formatter)))
        .leftMap(err => cannotParseWithMsg("bucket", err.getMessage))
      

    def encode(b: Bucket): String = b.value.format(formatter)

    private val formatter = DateTimeFormatter.ofPattern(DateFormat.seconds)
  }

  type DateTime = LocalDateTime
  object DateTime {
    def parse(s: String): Either[String, DateTime]
      = Either.catchNonFatal(LocalDateTime.parse(s))
        .leftMap(err => cannotParseWithMsg("datetime", err.getMessage))
    
    def encode(dt: DateTime): String = dt.format(formatter)

    def getBucket(dt: DateTime): Bucket = Bucket(Utils.roundToMinutes(dt))
    
    private val formatter = DateTimeFormatter.ofPattern(DateFormat.millis)
  }

  final case class TimeRange(from: DateTime, to: DateTime) {
    def contains(ts: Timestamp): Boolean
      = (!from.isAfter(ts.value)) && ts.value.isBefore(to)

    def getBuckets: Vector[Bucket] = {

      val numBuckets = ChronoUnit.MINUTES.between(from, to)

      val first = DateTime.getBucket(from)

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
        
        from  <- DateTime.parse(fromS)
        to    <- DateTime.parse(toS)
        
      } yield TimeRange(from, to)
    }

    def encode(tr: TimeRange): String = {
      val fromS = DateTime.encode(tr.from)
      val toS   = DateTime.encode(tr.to)
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
  final case class ProductId(value: Int) extends AnyVal

  
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

    def addOne(tag: UserTag): SimpleProfile = SimpleProfile(insertTagInOrder(tags, tag))

    def update(tag: UserTag, tagsToKeep: Int): SimpleProfile = addOne(tag).limit(tagsToKeep)
    def limit(tagsToKeep: Int): SimpleProfile = SimpleProfile(tags.take(tagsToKeep))

    def ++(other: SimpleProfile): SimpleProfile = {
      val newTags = other.tags.foldLeft(tags)(insertTagInOrder)
      SimpleProfile(newTags)
    }

    def prettify(cookie: Cookie): PrettyProfile = {
      val (views, buys) = tags.partition(_.action == VIEW)
      PrettyProfile(cookie, views, buys)
    }

    private def insertTagInOrder(tags: Vector[UserTag], tag: UserTag): Vector[UserTag] = {
      val (prefix, postfix) = tags.span(_.time.isAfter(tag.time))
      prefix ++ Vector(tag) ++ postfix
    }
  }

  object SimpleProfile {
    val default: SimpleProfile = SimpleProfile(Vector())
  }

  final case class PrettyProfile(cookie: Cookie, views: Vector[UserTag], buys: Vector[UserTag]) {
    def simplify: SimpleProfile = SimpleProfile(sortTags(views ++ buys))

    private def sortTags(tags: Vector[UserTag]): Vector[UserTag]
      = tags.sortWith((t1, t2) => t1.time.isAfter(t2.time))
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

  final case class AggregateValue(count: Int, sumPrice: Price) {
    def update(price: Price): AggregateValue = AggregateValue(count + 1, sumPrice + price)
    def +(other: AggregateValue): AggregateValue = AggregateValue(count + other.count, sumPrice + other.sumPrice)
  }
  object AggregateValue {
    val default: AggregateValue = AggregateValue(0, Price(0))
    def fromTag(tag: UserTag): AggregateValue = default.update(tag.productInfo.price)
  }

  // Aggregated Views and Buys
  final case class AggregateVB(views: AggregateValue, buys: AggregateValue) {
    def update(action: Action, price: Price): AggregateVB = action match {
      case VIEW => AggregateVB(views.update(price), buys)
      case BUY  => AggregateVB(views, buys.update(price))
    }

    def +(other: AggregateVB): AggregateVB = AggregateVB(views + other.views, buys + other.buys)

    def getValue(action: Action): AggregateValue = action match {
      case VIEW => views
      case BUY  => buys
    }
  }
  object AggregateVB {
    val default: AggregateVB = AggregateVB(AggregateValue.default, AggregateValue.default)
    def fromTag(tag: UserTag): AggregateVB = default.update(tag.action, tag.productInfo.price)
  }


  final case class AggregateKey(
    bucket:     Bucket,
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
      } yield AggregateKey(bucket, optOrigin, optBrandId, optCategoryId)
    }

    def fromFields(bucket: Bucket, fields: AggregateFields): AggregateKey
      = AggregateKey(bucket, fields.origin, fields.brandId, fields.categoryId)
  }

  type Generation = Int
  object Generation {
    val default: Generation = 0
  }

  final case class TrackGen[T](value: T, generation: Int) {
    def map[TT](f: T => TT) = TrackGen(f(value), generation)
    def traverse[F[_]: Functor, TT](f: T => F[TT]): F[TrackGen[TT]]
      = f(value).map(v => TrackGen[TT](v, generation))
  }

  object TrackGen {
    def default[T](value: T): TrackGen[T] = TrackGen(value, 0)
  }

  final case class TrackGenT[F[_], T](value: TrackGen[F[T]]) {
    def map[TT](f: T => TT)(implicit ev: Functor[F]) = TrackGenT(value.map[F[TT]](_.map(f)))
    def flatMap[TT](f: T => F[TT])(implicit ev: FlatMap[F]) = TrackGenT(value.map[F[TT]](_.flatMap(f)))
  }

  object TrackGenT {
    def default[F[_], T](value: F[T]) = TrackGenT(TrackGen.default(value))
  }
  


}
