package io.github.JankaGramofonomanka.analyticsplatform.common

import cats.effect.{Async, Resource, ExitCode}
import cats.syntax.all._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.Routes
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.EntityCodec


object FrontendServer {

  def stream[F[_]: Async](
    profiles: ProfilesDB[F],
    aggregates: AggregatesDB[F],
    tagsToAggregate: Topic.Publisher[F, UserTag],
    codec: EntityCodec[F]
  ): Stream[F, Nothing] = {
    val ops = new FrontendOps[F](profiles, aggregates, tagsToAggregate)

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract segments not checked
    // in the underlying routes.
    val httpApp = (
      Routes.kvRoutes(ops, codec)
    ).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)
    
    for {
      exitCode <- Stream.resource[F, ExitCode](
        EmberServerBuilder.default[F]
          .withHost(Config.Frontend.host)
          .withPort(Config.Frontend.port)
          .withHttpApp(finalHttpApp)
          .build >>
        Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
