package io.github.JankaGramofonomanka.analyticsplatform.common

import cats.effect.{Async, Resource, ExitCode}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Routes
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.Topic
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
    
    // TODO move literals somewhere
    for {
      exitCode <- Stream.resource[F, ExitCode](
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build >>
        Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
