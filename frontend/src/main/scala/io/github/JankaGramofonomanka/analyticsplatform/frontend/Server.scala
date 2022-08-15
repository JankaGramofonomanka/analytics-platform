package io.github.JankaGramofonomanka.analyticsplatform.frontend

import cats.effect.{Async, Resource, ExitCode}
import cats.syntax.all._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.Topic
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Config
import io.github.JankaGramofonomanka.analyticsplatform.frontend.Routes
import io.github.JankaGramofonomanka.analyticsplatform.frontend.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs.EntityCodec


object Server {

  def stream[F[_]: Async](
    profiles:         KeyValueDB[F, Cookie, SimpleProfile],
    aggregates:       KeyValueDB[F, AggregateKey, AggregateValue],
    tagsToAggregate:  Topic.Publisher[F, UserTag],
  )(implicit
    env: Config.Environment,
    entityCodec: EntityCodec[F]
  ): Stream[F, Nothing] = {
    val ops = new FrontendOps[F](profiles, aggregates, tagsToAggregate)

    val httpApp = (Routes.routes(ops)).orNotFound

    // With Middlewares in place
    val finalHttpApp = if (env.USE_LOGGER) Logger.httpApp(true, true)(httpApp) else httpApp
    
    for {
      exitCode <- Stream.resource[F, ExitCode](
        EmberServerBuilder.default[F]
          .withHost(Config.Frontend.getHost)
          .withPort(Config.Frontend.getPort)
          .withHttpApp(finalHttpApp)
          .build >>
        Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
