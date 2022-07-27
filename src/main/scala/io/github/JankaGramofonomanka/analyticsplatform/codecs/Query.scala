package io.github.JankaGramofonomanka.analyticsplatform.codecs

import org.http4s._
import org.http4s.dsl.io._
import cats.data.Validated
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.Data._

object Query {
  
  object CookieVar {
    def unapply(value: String): Option[Cookie] = Some(Cookie(value))
  }

  // Limit --------------------------------------------------------------------
  implicit val limitDecoder: QueryParamDecoder[Limit] = { param =>
    Validated
      .catchNonFatal(param.value.toInt)
      .leftMap(t => ParseFailure("Failed to decode limit", t.getMessage))
      .toValidatedNel
  }

  object OptLimitMatcher extends OptionalQueryParamDecoderMatcher[Limit](name = "limit")

  
  
  
  // TimeRange ----------------------------------------------------------------
  implicit val timeRangeDecoder: QueryParamDecoder[TimeRange] = { param =>
    parseTimeRange(param.value)
      .toValidated.leftMap(s => ParseFailure("Cannot parse time range", s)).toValidatedNel
    
  }

  object TimeRangeMatcher extends QueryParamDecoderMatcher[TimeRange](name = "time_range")




  // Action -------------------------------------------------------------------
  implicit val actionDecoder: QueryParamDecoder[Action] = { param => param.value match {
    case "VIEW" => Validated.Valid(VIEW).toValidatedNel
    case "BUY"  => Validated.Valid(BUY).toValidatedNel
    case s      => Validated.Invalid(ParseFailure("Failed to decode action", s"unkown action: $s")).toValidatedNel
  } }

  object ActionMatcher extends QueryParamDecoderMatcher[Action](name = "action")




  // Aggregates ---------------------------------------------------------------
  implicit val aggregateDecoder: QueryParamDecoder[Aggregate] = { param => param.value match {
    case "COUNT"      => Validated.Valid(COUNT).toValidatedNel
    case "SUM_PRICE"  => Validated.Valid(SUM_PRICE).toValidatedNel
    case s            => Validated.Invalid(ParseFailure("Failed to decode aggregates", s"unknown aggregate: $s")).toValidatedNel
  } }

  object AggregateMatcher extends QueryParamDecoderMatcher[Aggregate](name = "aggregates")
  object OptAggregateMatcher extends OptionalQueryParamDecoderMatcher[Aggregate](name = "aggregates")


  // Origin -------------------------------------------------------------------
  implicit val originDecoder: QueryParamDecoder[Origin] = { param => Validated.Valid(Origin(param.value)) }
  object OptOriginMatcher extends OptionalQueryParamDecoderMatcher[Origin](name = "origin")

  // BrandId ------------------------------------------------------------------
  implicit val brandIdDecoder: QueryParamDecoder[BrandId] = { param => Validated.Valid(BrandId(param.value)) }
  object OptBrandIdMatcher extends OptionalQueryParamDecoderMatcher[BrandId](name = "brand_id")

  // CategoryId ---------------------------------------------------------------
  implicit val categoryIdDecoder: QueryParamDecoder[CategoryId] = { param => Validated.Valid(CategoryId(param.value)) }
  object OptCategoryIdMatcher extends OptionalQueryParamDecoderMatcher[CategoryId](name = "category_id")
}
