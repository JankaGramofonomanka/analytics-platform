package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

import io.github.JankaGramofonomanka.analyticsplatform.KVFrontend
import io.github.JankaGramofonomanka.analyticsplatform.codecs.JsonCodec


object AnalyticsplatformServer {

  def stream[F[_]: Async](db: KeyValueDB[F], topic: TagTopic[F], codec: JsonCodec[F]): Stream[F, Nothing] = {
    for {
      client <- Stream.resource(EmberClientBuilder.default[F].build)
      helloWorldAlg = HelloWorld.impl[F]
      jokeAlg = Jokes.impl[F](client)

      ops = new KVFrontend[F](db, topic)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = (
        AnalyticsplatformRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
        AnalyticsplatformRoutes.jokeRoutes[F](jokeAlg) <+>
        AnalyticsplatformRoutes.kvRoutes(ops, codec)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

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
