package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.{ExitCode, IO, IOApp}

import io.github.JankaGramofonomanka.analyticsplatform.KV.Mock
import io.github.JankaGramofonomanka.analyticsplatform.codecs.IOEntityCodec

object Main extends IOApp {
  def run(args: List[String]) =
    AnalyticsplatformServer.stream[IO](Mock.DB, Mock.Topic, IOEntityCodec).compile.drain.as(ExitCode.Success)
}
