package io.github.JankaGramofonomanka.analyticsplatform.common.kv.db

import cats.data.OptionT
import cats.effect.IO


import com.aerospike.client.{AerospikeClient, Key, Record, Bin}
import com.aerospike.client.policy.{Policy, WritePolicy}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.db.{ProfilesDB, AggregatesDB}
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

    private val codec = AerospikeCodec

    private def mkKey(key: String): Key = new Key(config.namespace, config.profilesSetName, key)

    private def getRecord(keyS: String): IO[Option[Record]] = {
      val key = mkKey(keyS)
      for {
        record <- IO.delay(client.get(config.readPolicy, key))
      } yield Utils.checkForNull(record)
    }

    private def getBytes(key: String, binName: String): IO[Option[Array[Byte]]] = {
      for {
        record <- OptionT(getRecord(key))
        obj <- OptionT.fromOption[IO](Utils.checkForNull(record.bins.get(binName)))
      } yield obj.asInstanceOf[Array[Byte]]
    }.value

    private def putBin(keyS: String, bin: Bin): IO[Unit] = {
      val key = mkKey(keyS)
      IO.delay(client.put(config.writePolicy, key, bin))
    }

    def getProfile(cookie: Cookie): IO[SimpleProfile] = for {
      bytes <- getBytes(cookie.value, config.profileBinName)
      profile <- IO.delay(bytes.flatMap(r => codec.decodeProfile(r)))
    } yield profile.getOrElse(SimpleProfile.default)

    def updateProfile(cookie: Cookie, profile: SimpleProfile): IO[Unit] = {
      val bin = new Bin(config.profileBinName, codec.encodeProfile(profile))
      putBin(cookie.value, bin)
    }

    def getAggregate(key: AggregateKey): IO[AggregateValue] = for {
      bytes <- getBytes(codec.encodeAggregateKey(key), config.aggregateBinName)
      value = bytes.flatMap(r => codec.decodeAggregateValue(r))

    } yield value.getOrElse(AggregateValue.default)

    def updateAggregate(key: AggregateKey, value: AggregateValue): IO[Unit] = {
      val bin = new Bin(config.aggregateBinName, codec.encodeAggregateValue(value))
      putBin(codec.encodeAggregateKey(key), bin)
    }

  }
  



}

