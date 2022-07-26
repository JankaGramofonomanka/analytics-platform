package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.{ExitCode, IO, IOApp}

import io.github.JankaGramofonomanka.analyticsplatform.KVMock
import io.github.JankaGramofonomanka.analyticsplatform.codecs.IOJsonCodec

object Main extends IOApp {
  def run(args: List[String]) =
    AnalyticsplatformServer.stream[IO](KVMock.DB, KVMock.Topic, IOJsonCodec).compile.drain.as(ExitCode.Success)
}
