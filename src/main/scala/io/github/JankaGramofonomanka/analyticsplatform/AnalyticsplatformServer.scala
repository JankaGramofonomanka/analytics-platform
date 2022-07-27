package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

import io.github.JankaGramofonomanka.analyticsplatform.KV.Routes
import io.github.JankaGramofonomanka.analyticsplatform.KV.FrontendOps
import io.github.JankaGramofonomanka.analyticsplatform.KV.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.KV.TagTopic
import io.github.JankaGramofonomanka.analyticsplatform.codecs.EntityCodec


object AnalyticsplatformServer {

  def stream[F[_]: Async](db: KeyValueDB[F], topic: TagTopic[F], codec: EntityCodec[F]): Stream[F, Nothing] = {
    val ops = new FrontendOps[F](db, topic)

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
      exitCode <- Stream.resource(
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
