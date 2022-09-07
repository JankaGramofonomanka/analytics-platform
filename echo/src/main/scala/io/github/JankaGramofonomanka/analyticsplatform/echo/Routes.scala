package io.github.JankaGramofonomanka.analyticsplatform.echo

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object Routes {

  def routes[F[_]: Sync] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {

      case GET -> Root / "health" => Ok()

      case  POST -> Root / "user_tags"
        => NoContent()
    
      case  req @ POST -> Root / "user_profiles" / _
        => Ok(req.body)
        
      case  req @ POST -> Root / "aggregates" :? _
        => Ok(req.body)
    }
  }

  
}


