package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import io.circe._
import io.circe.syntax._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._


import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
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
  
  implicit val priceDecoder:      Decoder[Price]      = Decoder.decodeLong.emap { s => Right(Price(s)) }
  implicit val priceEncoder:      Encoder[Price]      = Encoder.encodeLong.contramap[Price](_.value)
  
  implicit val productIdDecoder:  Decoder[ProductId]  = Decoder.decodeInt.emap { i => Right(ProductId(i)) }
  implicit val productIdEncoder:  Encoder[ProductId]  = Encoder.encodeInt.contramap[ProductId](_.value)


  implicit val productInfoDecoder:  Decoder[ProductInfo]  = deriveConfiguredDecoder[ProductInfo]
  implicit val productInfoEncoder:  Encoder[ProductInfo]  = deriveConfiguredEncoder[ProductInfo]

  implicit val userTagDecoder:      Decoder[UserTag]      = deriveConfiguredDecoder[UserTag]
  implicit val userTagEncoder:      Encoder[UserTag]      = deriveConfiguredEncoder[UserTag]

  implicit val profileDecoder:      Decoder[Profile]      = deriveConfiguredDecoder[Profile]
  implicit val profileEncoder:      Encoder[Profile]      = deriveConfiguredEncoder[Profile]


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
    
    val aggregateColumns = fields.aggregates.map {
      case SUM_PRICE  => aggregatesFieldNames.sumPrice.asJson
      case COUNT      => aggregatesFieldNames.count.asJson
    }
    
    val columns = List(
      Some(aggregatesFieldNames.bucket),
      Some(aggregatesFieldNames.action),
      mOriginCol,
      mBrandIdCol,
      mCategoryIdCol,
    ).flatten.map(_.asJson) ++ aggregateColumns

    Json.fromValues(columns)

  }

  private def mkRows(aggregates: Aggregates): Json = {
    val fields = List(
      Some(aggregates.fields.action.asJson),
      aggregates.fields.origin    .map(_.asJson),
      aggregates.fields.brandId   .map(_.asJson),
      aggregates.fields.categoryId.map(_.asJson),
    ).flatten

    Json.fromValues(aggregates.items.map(mkRow(fields, aggregates.fields.aggregates)))
  }

  private def mkRow(
    fields: List[Json],
    aggregates: List[Aggregate]
  )(item: AggregateItem): Json = {

    val bucket    = List(item.bucket.asJson)
    val sumPrice  = item.value.sumPrice.value.toString.asJson
    val count     = item.value.count.toString.asJson
    val aggregateValues = aggregates.map {
      case SUM_PRICE  => sumPrice
      case COUNT      => count
    }

    Json.fromValues(bucket ++ fields ++ aggregateValues)
  }

  implicit val aggregateKeyEncoder: Encoder[AggregateKey] = new Encoder[AggregateKey] {

    private val noneJson: Json = 0.asJson
    
    def apply(key: AggregateKey): Json = {
      val bucketJson = key.bucket.getSeconds.asJson
      Json.arr(
        bucketJson,

        key.origin    .map(_.asJson).getOrElse(noneJson),
        key.brandId   .map(_.asJson).getOrElse(noneJson),
        key.categoryId.map(_.asJson).getOrElse(noneJson),
      )
    }
  }


  implicit val aggregateValueEncoder: Encoder[AggregateValue] = deriveConfiguredEncoder[AggregateValue]
  implicit val aggregateValueDecoder: Decoder[AggregateValue] = deriveConfiguredDecoder[AggregateValue]

  implicit val aggregateVBEncoder: Encoder[AggregateVB]
    = Encoder.encodeTuple4[Int, Long, Int, Long].contramap[AggregateVB] {
      vb => (vb.views.count, vb.views.sumPrice.value, vb.buys.count, vb.buys.sumPrice.value)
    }
    
  implicit val aggregateVBDecoder: Decoder[AggregateVB]
    = Decoder.decodeTuple4[Int, Long, Int, Long].map {
      Utils.uncurry4[Int, Long, Int, Long, AggregateVB] {
        (vCount: Int, vSumPrice: Long, bCount: Int, bSumPrice: Long) => AggregateVB(
          AggregateValue(vCount, Price(vSumPrice)),
          AggregateValue(bCount, Price(bSumPrice)),
        )
      }
    }
    
  
}


