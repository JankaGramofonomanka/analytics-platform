package io.github.JankaGramofonomanka.analyticsplatform.echo

import cats.effect.{ExitCode, IO, IOApp}


import io.github.JankaGramofonomanka.analyticsplatform.echo.Server

object Main extends IOApp {
  
  def run(args: List[String]) = {

    
    Server.stream[IO].compile.drain.as(ExitCode.Success)
  }
}
