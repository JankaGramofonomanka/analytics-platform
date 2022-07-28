package io.github.JankaGramofonomanka.analyticsplatform.KV

import cats.effect.Sync
import cats.data.Validated.{Valid, Invalid}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import io.github.JankaGramofonomanka.analyticsplatform.Data._
import io.github.JankaGramofonomanka.analyticsplatform.KV.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.codecs.Query._
import io.github.JankaGramofonomanka.analyticsplatform.codecs.EntityCodec

object Routes {

  def kvRoutes[F[_]: Sync](ops: FrontendOps[F], codec: EntityCodec[F]) = {

    import codec._

    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case  req @ POST -> Root / "user_tags"
        => (req.as[UserTag] flatMap ops.storeTag _) *> NoContent()
    
      case  POST -> Root / "user_profiles" / CookieVar(cookie)
                                          :? TimeRangeMatcher(timeRange)
                                          +& OptLimitMatcher(optLimit)
        => {
          // TODO: replace 200 with a constant or whatever
          val limit = optLimit.getOrElse(200)
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
            ops.getAggregates(
              timeRange,
              action,
              count,
              sumPrice,
              origin,
              brandId,
              categoryId,
            ) flatMap (x => Ok(x))
          }

          // TODO move message to errors file or sth
          case Invalid(_) => BadRequest("Cannot parse parameter(s): `aggregates`")
          
        }
    }
  }
}


