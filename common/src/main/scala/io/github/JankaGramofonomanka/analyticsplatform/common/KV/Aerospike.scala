package io.github.JankaGramofonomanka.analyticsplatform.common.KV

import cats.effect.IO


import com.aerospike.client.{AerospikeClient, Key, Record, Bin}
import com.aerospike.client.policy.{Policy, WritePolicy}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils._
import io.github.JankaGramofonomanka.analyticsplatform.common.KV.{ProfilesDB, AggregatesDB}
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec


object Aerospike {

  final case class Config(
    readPolicy: Policy,
    writePolicy: WritePolicy,
    namespace: String,
    profilesSetName: String,
    aggregatesSetName: String,
    profileBinName: String,
    aggregateBinName: String,
  )
  
  

  class DB(client: AerospikeClient, config: Config) extends ProfilesDB[IO] with AggregatesDB[IO] {

    private val codec = new AerospikeCodec(config.profileBinName, config.aggregateBinName)

    private def mkKey(key: String): Key = new Key(config.namespace, config.profilesSetName, key)

    private def getRecord(keyS: String): IO[Option[Record]] = {
      val key = mkKey(keyS)
      for {
        record <- IO.delay(client.get(config.readPolicy, key))
      } yield checkForNull(record)
    }

    private def putBin(keyS: String, bin: Bin): IO[Unit] = {
      val key = mkKey(keyS)
      IO.delay(client.put(config.writePolicy, key, bin))
    }

    def getProfile(cookie: Cookie): IO[SimpleProfile] = for {
      record <- getRecord(cookie.value)
      profile <- IO.delay(record.flatMap(r => codec.decodeProfile(r)))
    } yield profile.getOrElse(SimpleProfile.default)

    def updateProfile(cookie: Cookie, profile: SimpleProfile): IO[Unit]
      = putBin(cookie.value, codec.encodeProfile(profile))

    def getAggregate(info: AggregateInfo): IO[AggregateValue] = for {
      record <- getRecord(codec.encodeAggregateInfo(info))
      value = record.flatMap(r => codec.decodeAggregateValue(r))

    } yield value.getOrElse(AggregateValue.default)

    def updateAggregate(info: AggregateInfo, value: AggregateValue): IO[Unit]
      = putBin(codec.encodeAggregateInfo(info), codec.encodeAggregateValue(value))

  }
  



}

