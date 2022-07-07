package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.{Sync}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl


import io.github.JankaGramofonomanka.analyticsplatform.Query._

object AnalyticsplatformRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  def debugModeRoutes[F[_]: Sync]: HttpRoutes[F] = {
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
}


