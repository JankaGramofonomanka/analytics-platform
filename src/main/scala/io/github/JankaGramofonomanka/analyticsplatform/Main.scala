package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    AnalyticsplatformServer.stream[IO].compile.drain.as(ExitCode.Success)
}
