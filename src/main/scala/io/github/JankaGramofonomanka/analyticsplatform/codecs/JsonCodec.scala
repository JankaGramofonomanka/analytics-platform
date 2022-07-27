package io.github.JankaGramofonomanka.analyticsplatform.codecs

import java.time.LocalDateTime

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._


import io.github.JankaGramofonomanka.analyticsplatform.Data._

object JsonCodec {

  
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val cookieDecoder: Decoder[Cookie] = Decoder.decodeString.emap { s => Right(Cookie(s)) }
  implicit val cookieEncoder: Encoder[Cookie] = Encoder.encodeString.contramap[Cookie](_.value)

  implicit val datetimeDecoder: Decoder[LocalDateTime] = Decoder.decodeString.emap { s => 
    parseLocalDateTime(s)
  }
  implicit val datetimeEncoder: Encoder[LocalDateTime] = Encoder.encodeString.contramap[LocalDateTime](_.toString)
  

  implicit val timeRangeDecoder: Decoder[TimeRange] = Decoder.decodeString.emap { s => parseTimeRange(s) }

  implicit val actionDecoder: Decoder[Action] = Decoder.decodeString.emap { 
    case "VIEW" => Right(VIEW)
    case "BUY"  => Right(BUY)
    case s      => Left(f"Unknown action: $s")
  }
  implicit val actionEncoder: Encoder[Action] = Encoder.encodeString.contramap[Action] {
    case VIEW => "VIEW"
    case BUY  => "BUY"
  }

  implicit val aggregateDecoder: Decoder[Aggregate] = Decoder.decodeString.emap { 
    case "COUNT"      => Right(COUNT)
    case "SUM_PRICE"  => Right(SUM_PRICE)
    case s      => Left(f"Unknown aggregate: $s")
  }
  implicit val aggregateEncoder: Encoder[Aggregate] = Encoder.encodeString.contramap[Aggregate] {
    case COUNT      => "COUNT"
    case SUM_PRICE  => "SUM_PRICE"
  }

  
  implicit val originDecoder:     Decoder[Origin]     = Decoder.decodeString.emap { s => Right(Origin(s)) }
  implicit val originEncoder:     Encoder[Origin]     = Encoder.encodeString.contramap[Origin](_.value)
  
  implicit val brandIdDecoder:    Decoder[BrandId]    = Decoder.decodeString.emap { s => Right(BrandId(s)) }
  implicit val brandIdEncoder:    Encoder[BrandId]    = Encoder.encodeString.contramap[BrandId](_.value)

  implicit val categoryIdDecoder: Decoder[CategoryId] = Decoder.decodeString.emap { s => Right(CategoryId(s)) }
  implicit val categoryIdEncoder: Encoder[CategoryId] = Encoder.encodeString.contramap[CategoryId](_.value)

  implicit val countryDecoder:    Decoder[Country]    = Decoder.decodeString.emap { s => Right(Country(s)) }
  implicit val countryEncoder:    Encoder[Country]    = Encoder.encodeString.contramap[Country](_.value)
  
  implicit val priceDecoder:      Decoder[Price]      = Decoder.decodeInt.emap { s => Right(Price(s)) }
  implicit val priceEncoder:      Encoder[Price]      = Encoder.encodeInt.contramap[Price](_.value)
  
  implicit val productIdDecoder:  Decoder[ProductId]  = Decoder.decodeString.emap { s => Right(ProductId(s)) }
  implicit val productIdEncoder:  Encoder[ProductId]  = Encoder.encodeString.contramap[ProductId](_.value)

  
  implicit val deviceDecoder: Decoder[Device] = Decoder.decodeString.emap { 
    case "PC"     => Right(PC)
    case "MOBILE" => Right(MOBILE)
    case "TV"     => Right(TV)
    case s        => Left(f"Unknown device: $s")
  }
  implicit val deviceEncoder: Encoder[Device] = Encoder.encodeString.contramap[Device] {
    case PC     => "PC"
    case MOBILE => "MOBILE"
    case TV     => "TV"
  }


  implicit val userTagDecoder       = deriveConfiguredDecoder[UserTag]
  implicit val userTagEncoder       = deriveConfiguredEncoder[UserTag]

  implicit val productInfoDecoder   = deriveConfiguredDecoder[ProductInfo]
  implicit val productInfoEncoder   = deriveConfiguredEncoder[ProductInfo]

  implicit val prettyProfileDecoder = deriveConfiguredDecoder[PrettyProfile]
  implicit val prettyProfileEncoder = deriveConfiguredEncoder[PrettyProfile]


  implicit val aggregatesEncoder = new Encoder[Aggregates] {
    final def apply(aggregates: Aggregates): Json = Json.obj(
      ("columns", mkColumns(aggregates.fields)),
      ("rows",    mkRows(aggregates)),
    )
  }

  private def mkColumns(fields: AggregateFields): Json = {
    val mOriginCol      = fields.origin     .map(_ => "origin")
    val mBrandIdCol     = fields.brandId    .map(_ => "brand_id")
    val mCategoryIdCol  = fields.categoryId .map(_ => "category_id")
    
    val mSumPriceCol  = if (fields.sumPrice)  Some("sum_price")  else None
    val mCountCol     = if (fields.count)     Some("count")      else None
    
    val columns = Vector(
      Some("1m_bucket"),
      Some("action"),
      mOriginCol,
      mBrandIdCol,
      mCategoryIdCol,
      mSumPriceCol,
      mCountCol,
    ).flatten.map(_.asJson)

    Json.fromValues(columns)
  }

  private def mkRows(aggregates: Aggregates): Json = {
    val fields = Vector(
      Some(aggregates.fields.action.asJson),
      aggregates.fields.origin    .map(_.asJson),
      aggregates.fields.brandId   .map(_.asJson),
      aggregates.fields.categoryId.map(_.asJson),
    ).flatten

    val includeSumPrice = aggregates.fields.sumPrice
    val includeCount    = aggregates.fields.count

    Json.fromValues(aggregates.values.map(mkRow(fields, includeSumPrice, includeCount)))
  }

  private def mkRow(
    fields: Vector[Json],
    includeSumPrice: Boolean,
    includeCount: Boolean,
  )(item: (Bucket, AggregateValue)): Json = {

    val bucket    = Vector(item._1.asJson)
    val sumPrice  = if (includeSumPrice)  Vector(item._2.sumPrice.asJson) else Vector()
    val count     = if (includeCount)     Vector(item._2.count.asJson)    else Vector()
    
    Json.fromValues(bucket ++ fields ++ sumPrice ++ count)
  }
  
}


