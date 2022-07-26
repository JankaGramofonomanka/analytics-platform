package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import io.github.JankaGramofonomanka.analyticsplatform.Data._
import io.github.JankaGramofonomanka.analyticsplatform.KVFrontend
import io.github.JankaGramofonomanka.analyticsplatform.codecs.Query._
import io.github.JankaGramofonomanka.analyticsplatform.codecs.JsonCodec

object AnalyticsplatformRoutes {

  def mockRoutes[F[_]: Sync]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case        POST -> Root / "user_tags" => NoContent()
      case req @  POST -> Root / "user_profiles" / CookieVar(_)
                                                :? TimeRangeMatcher(_)
                                                +& OptLimitMatcher(_)
        => Ok(req.body)
      
      case req @  POST -> Root / "aggregates" :? TimeRangeMatcher(_)
                                              +& ActionMatcher(_)
                                              +& AggregateMatcher(_)
                                              +& OptAggregateMatcher(_)
                                              +& OptOriginMatcher(_)
                                              +& OptBrandIdMatcher(_)
                                              +& OptCategoryIdMatcher(_)
        => Ok(req.body)
    }
  }

  def kvRoutes[F[_]: Sync](ops: KVFrontend[F], codec: JsonCodec[F]) = {

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
                                        +& AggregateMatcher(aggregate1)
                                        +& OptAggregateMatcher(optAggregate2)
                                        +& OptOriginMatcher(origin)
                                        +& OptBrandIdMatcher(brandId)
                                        +& OptCategoryIdMatcher(categoryId)
        => {
          val aggregates = List(Some(aggregate1), optAggregate2).flatten.distinct
          val count     = if (aggregates.contains(COUNT)) true else false
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

    }

  }
}


