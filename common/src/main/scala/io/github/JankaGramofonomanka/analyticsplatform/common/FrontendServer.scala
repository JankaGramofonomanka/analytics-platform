package io.github.JankaGramofonomanka.analyticsplatform.common

import cats.effect.{Async, Resource, ExitCode}
import cats.syntax.all._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.Environment
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.Routes
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.topic.Topic
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.EntityCodec


object FrontendServer {

  def stream[F[_]: Async](
    profiles:         KeyValueDB[F, Cookie, SimpleProfile],
    aggregates:       KeyValueDB[F, AggregateKey, AggregateValue],
    tagsToAggregate:  Topic.Publisher[F, UserTag],
  )(implicit
    env: Environment,
    entityCodec: EntityCodec[F]
  ): Stream[F, Nothing] = {
    val ops = new FrontendOps[F](profiles, aggregates, tagsToAggregate)

    val httpApp = (Routes.kvRoutes(ops)).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)
    
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
