package io.github.JankaGramofonomanka.analyticsplatform.echo


import cats.effect.{Async, Resource, ExitCode}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger


import io.github.JankaGramofonomanka.analyticsplatform.echo.Routes

object Server {

  def stream[F[_]: Async]: Stream[F, Nothing] = {
    

    val httpApp = (Routes.routes).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)
    
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
