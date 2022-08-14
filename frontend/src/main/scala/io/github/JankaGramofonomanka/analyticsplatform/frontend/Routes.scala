package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.Sync
import cats.data.Validated.{Valid, Invalid}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ErrorMessages._
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Config
import io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs.QueryCodec._
import io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs.EntityCodec
import io.github.JankaGramofonomanka.analyticsplatform.frontend.FrontendOps

object Routes {

  def routes[F[_]: Sync](ops: FrontendOps[F])(implicit env: Config.Environment, codec: EntityCodec[F]) = {

    import codec._

    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {

      case GET -> Root / "health" => Ok()

      case  req @ POST -> Root / "user_tags"
        => (req.as[UserTag] flatMap ops.storeTag _) *> NoContent()
    
      case  POST -> Root / "user_profiles" / CookieVar(cookie)
                                          :? TimeRangeMatcher(timeRange)
                                          +& OptLimitMatcher(optLimit)
        => {
          val limit = optLimit.getOrElse(env.DEFAULT_LIMIT)
          ops.getProfile(cookie, timeRange, limit) flatMap (x => Ok(x))
        }
        
      case  POST -> Root / "aggregates" :? TimeRangeMatcher(timeRange)
                                        +& ActionMatcher(action)
                                        +& AggregatesMatcher(aggregates)
                                        +& OptOriginMatcher(origin)
                                        +& OptBrandIdMatcher(brandId)
                                        +& OptCategoryIdMatcher(categoryId)
        => aggregates match {

          case Valid(aggregates) => {
            val count     = if (aggregates.contains(COUNT))     true else false
            val sumPrice  = if (aggregates.contains(SUM_PRICE)) true else false
            val fields = AggregateFields(
              action,
              count,
              sumPrice,
              origin,
              brandId,
              categoryId,
            )
            ops.getAggregates(timeRange, fields) flatMap (x => Ok(x))
          }

          case Invalid(_) => BadRequest(failedToDecodeParameterS(Config.Query.ParamNames.aggregates))
          
        }
    }
  }
}


