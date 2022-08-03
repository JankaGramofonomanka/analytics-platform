package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import org.http4s._
import org.http4s.dsl.io._
import cats.data.Validated
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ErrorMessages._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config.QueryParams

object Query {
  
  object CookieVar {
    def unapply(value: String): Option[Cookie] = Some(Cookie(value))
  }

  // Limit --------------------------------------------------------------------
  implicit val limitDecoder: QueryParamDecoder[Limit] = { param =>
    Validated
      .catchNonFatal(param.value.toInt)
      .leftMap(t => ParseFailure(failedToDecodeParameter(QueryParams.limit), t.getMessage))
      .toValidatedNel
  }

  object OptLimitMatcher extends OptionalQueryParamDecoderMatcher[Limit](name = QueryParams.limit)

  
  
  
  // TimeRange ----------------------------------------------------------------
  implicit val timeRangeDecoder: QueryParamDecoder[TimeRange] = { param =>
    TimeRange.parse(param.value)
      .toValidated.leftMap(s => ParseFailure(failedToDecodeParameter(QueryParams.timeRange), s))
      .toValidatedNel
    
  }

  object TimeRangeMatcher extends QueryParamDecoderMatcher[TimeRange](name = QueryParams.timeRange)




  // Action -------------------------------------------------------------------
  implicit val actionDecoder: QueryParamDecoder[Action] = { param =>
    Action.parse(param.value)
      .toValidated.leftMap(s => ParseFailure(failedToDecodeParameter(QueryParams.action), s))
      .toValidatedNel
  }

  object ActionMatcher extends QueryParamDecoderMatcher[Action](name = QueryParams.action)




  // Aggregates ---------------------------------------------------------------
  implicit val aggregateDecoder: QueryParamDecoder[Aggregate] = { param =>
    Aggregate.parse(param.value)
      .toValidated.leftMap(s => ParseFailure(failedToDecodeParameter(QueryParams.aggregates), s))
      .toValidatedNel
  }

  object AggregatesMatcher extends OptionalMultiQueryParamDecoderMatcher[Aggregate](name = QueryParams.aggregates)


  // Origin -------------------------------------------------------------------
  implicit val originDecoder: QueryParamDecoder[Origin] = { param => Validated.Valid(Origin(param.value)) }
  object OptOriginMatcher extends OptionalQueryParamDecoderMatcher[Origin](name = QueryParams.origin)

  // BrandId ------------------------------------------------------------------
  implicit val brandIdDecoder: QueryParamDecoder[BrandId] = { param => Validated.Valid(BrandId(param.value)) }
  object OptBrandIdMatcher extends OptionalQueryParamDecoderMatcher[BrandId](name = QueryParams.brandId)

  // CategoryId ---------------------------------------------------------------
  implicit val categoryIdDecoder: QueryParamDecoder[CategoryId] = { param => Validated.Valid(CategoryId(param.value)) }
  object OptCategoryIdMatcher extends OptionalQueryParamDecoderMatcher[CategoryId](name = QueryParams.categoryId)
}
