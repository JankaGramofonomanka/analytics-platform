package io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs

import org.http4s._
import org.http4s.dsl.io._
import cats.data.Validated
import cats.implicits._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ErrorMessages._
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Config.Query.ParamNames

object QueryCodec {
  
  object CookieVar {
    def unapply(value: String): Option[Cookie] = Some(Cookie(value))
  }

  // Limit --------------------------------------------------------------------
  implicit val limitDecoder: QueryParamDecoder[Limit] = { param =>
    Validated
      .catchNonFatal(param.value.toInt)
      .leftMap(t => ParseFailure(failedToDecodeParameter(ParamNames.limit), t.getMessage))
      .toValidatedNel
  }

  object OptLimitMatcher extends OptionalQueryParamDecoderMatcher[Limit](name = ParamNames.limit)

  
  
  
  // TimeRange ----------------------------------------------------------------
  implicit val timeRangeDecoder: QueryParamDecoder[TimeRange] = { param =>
    TimeRange.parse(param.value)
      .toValidated.leftMap(s => ParseFailure(failedToDecodeParameter(ParamNames.timeRange), s))
      .toValidatedNel
    
  }

  object TimeRangeMatcher extends QueryParamDecoderMatcher[TimeRange](name = ParamNames.timeRange)




  // Action -------------------------------------------------------------------
  implicit val actionDecoder: QueryParamDecoder[Action] = { param =>
    Action.parse(param.value)
      .toValidated.leftMap(s => ParseFailure(failedToDecodeParameter(ParamNames.action), s))
      .toValidatedNel
  }

  object ActionMatcher extends QueryParamDecoderMatcher[Action](name = ParamNames.action)




  // Aggregates ---------------------------------------------------------------
  implicit val aggregateDecoder: QueryParamDecoder[Aggregate] = { param =>
    Aggregate.parse(param.value)
      .toValidated.leftMap(s => ParseFailure(failedToDecodeParameter(ParamNames.aggregates), s))
      .toValidatedNel
  }

  object AggregatesMatcher extends OptionalMultiQueryParamDecoderMatcher[Aggregate](name = ParamNames.aggregates)


  // Origin -------------------------------------------------------------------
  implicit val originDecoder: QueryParamDecoder[Origin] = { param => Validated.Valid(Origin(param.value)) }
  object OptOriginMatcher extends OptionalQueryParamDecoderMatcher[Origin](name = ParamNames.origin)

  // BrandId ------------------------------------------------------------------
  implicit val brandIdDecoder: QueryParamDecoder[BrandId] = { param => Validated.Valid(BrandId(param.value)) }
  object OptBrandIdMatcher extends OptionalQueryParamDecoderMatcher[BrandId](name = ParamNames.brandId)

  // CategoryId ---------------------------------------------------------------
  implicit val categoryIdDecoder: QueryParamDecoder[CategoryId] = { param => Validated.Valid(CategoryId(param.value)) }
  object OptCategoryIdMatcher extends OptionalQueryParamDecoderMatcher[CategoryId](name = ParamNames.categoryId)
}
