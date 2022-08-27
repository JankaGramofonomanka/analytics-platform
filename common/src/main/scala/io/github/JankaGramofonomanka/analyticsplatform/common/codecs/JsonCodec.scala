package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import io.circe._
import io.circe.syntax._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._


import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config

object JsonCodec {

  

  implicit val codecConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val cookieDecoder: Decoder[Cookie] = Decoder.decodeString.emap { s => Right(Cookie(s)) }
  implicit val cookieEncoder: Encoder[Cookie] = Encoder.encodeString.contramap[Cookie](_.value)

  implicit val timestampDecoder: Decoder[Timestamp]
    = Decoder.decodeString.emap { s => Timestamp.parse(s) }
  implicit val timestampEncoder: Encoder[Timestamp]
    = Encoder.encodeString.contramap[Timestamp](ts => Timestamp.encode(ts))

  implicit val bucketDecoder: Decoder[Bucket]
    = Decoder.decodeString.emap { s => Bucket.parse(s) }
  implicit val bucketEncoder: Encoder[Bucket]
    = Encoder.encodeString.contramap[Bucket](ts => Bucket.encode(ts))  

  implicit val timeRangeDecoder: Decoder[TimeRange] = Decoder.decodeString.emap { s => TimeRange.parse(s) }
  implicit val timeRangeEncoder: Encoder[TimeRange]
    = Encoder.encodeString.contramap { tr => TimeRange.encode(tr) }

  implicit val actionDecoder: Decoder[Action] = Decoder.decodeString.emap(s => Action.parse(s))
  implicit val actionEncoder: Encoder[Action]
    = Encoder.encodeString.contramap[Action](action => Action.encode(action))

  implicit val aggregateDecoder: Decoder[Aggregate] = Decoder.decodeString.emap(s => Aggregate.parse(s))
  implicit val aggregateEncoder: Encoder[Aggregate]
    = Encoder.encodeString.contramap[Aggregate](aggregate => Aggregate.encode(aggregate))

  implicit val deviceDecoder: Decoder[Device] = Decoder.decodeString.emap(s => Device.parse(s))
  implicit val deviceEncoder: Encoder[Device]
    = Encoder.encodeString.contramap[Device](device => Device.encode(device))

  
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
  
  implicit val productIdDecoder:  Decoder[ProductId]  = Decoder.decodeInt.emap { i => Right(ProductId(i)) }
  implicit val productIdEncoder:  Encoder[ProductId]  = Encoder.encodeInt.contramap[ProductId](_.value)


  implicit val productInfoDecoder:    Decoder[ProductInfo]    = deriveConfiguredDecoder[ProductInfo]
  implicit val productInfoEncoder:    Encoder[ProductInfo]    = deriveConfiguredEncoder[ProductInfo]

  implicit val userTagDecoder:        Decoder[UserTag]        = deriveConfiguredDecoder[UserTag]
  implicit val userTagEncoder:        Encoder[UserTag]        = deriveConfiguredEncoder[UserTag]

  implicit val simpleProfileDecoder:  Decoder[SimpleProfile]  = deriveConfiguredDecoder[SimpleProfile]
  implicit val simpleProfileEncoder:  Encoder[SimpleProfile]  = deriveConfiguredEncoder[SimpleProfile]

  implicit val prettyProfileDecoder:  Decoder[PrettyProfile]  = deriveConfiguredDecoder[PrettyProfile]
  implicit val prettyProfileEncoder:  Encoder[PrettyProfile]  = deriveConfiguredEncoder[PrettyProfile]


  private val aggregatesFieldNames = Config.Aggregates.Fields
  implicit val aggregatesEncoder = new Encoder[Aggregates] {
    final def apply(aggregates: Aggregates): Json = Json.obj(
      (aggregatesFieldNames.columns,  mkColumns(aggregates.fields)),
      (aggregatesFieldNames.rows,     mkRows(aggregates)),
    )
  }

  private def mkColumns(fields: AggregateFields): Json = {

    val mOriginCol      = fields.origin     .map(_ => aggregatesFieldNames.origin)
    val mBrandIdCol     = fields.brandId    .map(_ => aggregatesFieldNames.brandId)
    val mCategoryIdCol  = fields.categoryId .map(_ => aggregatesFieldNames.categoryId)
    
    val mSumPriceCol  = if (fields.sumPrice)  Some(aggregatesFieldNames.sumPrice) else None
    val mCountCol     = if (fields.count)     Some(aggregatesFieldNames.count)    else None
    
    val columns = Vector(
      Some(aggregatesFieldNames.bucket),
      Some(aggregatesFieldNames.action),
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

    Json.fromValues(aggregates.items.map(mkRow(fields, includeSumPrice, includeCount)))
  }

  private def mkRow(
    fields: Vector[Json],
    includeSumPrice: Boolean,
    includeCount: Boolean,
  )(item: AggregateItem): Json = {

    val bucket    = Vector(item.bucket.asJson)
    val sumPrice  = if (includeSumPrice)  Vector(item.value.sumPrice.asJson) else Vector()
    val count     = if (includeCount)     Vector(item.value.count.asJson)    else Vector()
    
    Json.fromValues(bucket ++ fields ++ sumPrice ++ count)
  }

  implicit val aggregateKeyEncoder: Encoder[AggregateKey] = new Encoder[AggregateKey] {

    private val noneJson: Json = 0.asJson
    
    def apply(key: AggregateKey): Json = {
      val bucketJson = key.bucket.getSeconds.asJson
      val actionJson = key.action match {
        case BUY  => 1.asJson
        case VIEW => 2.asJson
      }
      Json.arr(
        bucketJson,
        actionJson,
        
        key.origin    .map(_.asJson).getOrElse(noneJson),
        key.brandId   .map(_.asJson).getOrElse(noneJson),
        key.categoryId.map(_.asJson).getOrElse(noneJson),
      )
    }
  }


  implicit val aggregateValueEncoder: Encoder[AggregateValue] = deriveConfiguredEncoder[AggregateValue]
  implicit val aggregateValueDecoder: Decoder[AggregateValue] = deriveConfiguredDecoder[AggregateValue]
  
}


