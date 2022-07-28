package io.github.JankaGramofonomanka.analyticsplatform.echo

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import io.github.JankaGramofonomanka.analyticsplatform.codecs.Query._

object Routes {

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
                                              +& AggregatesMatcher(_)
                                              +& OptOriginMatcher(_)
                                              +& OptBrandIdMatcher(_)
                                              +& OptCategoryIdMatcher(_)
        => Ok(req.body)
    }
  }

}


