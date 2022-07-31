package io.github.JankaGramofonomanka.analyticsplatform

import cats.effect.{ExitCode, IO, IOApp}

import com.aerospike.client.{AerospikeClient, Host}
import com.aerospike.client.policy.{Policy, WritePolicy, ClientPolicy}

import io.github.JankaGramofonomanka.analyticsplatform.KV.Mock
import io.github.JankaGramofonomanka.analyticsplatform.KV.Aerospike
import io.github.JankaGramofonomanka.analyticsplatform.codecs.IOEntityCodec

object Main extends IOApp {
  def run(args: List[String]) =
    {
      // TODO Move literals somewhere
      val host = new Host("localhost", 3000)

      // TODO specify policies
      val config = Aerospike.Config(
        new Policy(),
        new WritePolicy(),
        "analyticsplatform",
        "profiles",
        "aggregates",
        "profile",
        "aggregate",
      )
      val client = new AerospikeClient(new ClientPolicy(), host)
      val profiles = new Aerospike.DB(client, config)
      AnalyticsplatformServer.stream[IO](profiles, Mock.DB, Mock.Topic, IOEntityCodec).compile.drain.as(ExitCode.Success)
    }
}
