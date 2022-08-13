package io.github.JankaGramofonomanka.analyticsplatform.common

import scala.util.Try

import cats.data.OptionT
import cats.effect.IO

import com.aerospike.client.{AerospikeClient, Key, Record, Bin, Operation}
import com.aerospike.client.policy.{Policy, WritePolicy}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec


object Aerospike {

  // TODO create case classes `Namespace`, `SetName` and `Key`
  // TODO replace this with `common.Config.Aerospike`
  final case class Config(
    readPolicy: Policy,
    writePolicy: WritePolicy,
    namespace: String,
    profilesSetName: String,
    aggregatesSetName: String,
    profileBinName: String,
    aggregateBinName: String,
  )
  
  

  class DB(client: AerospikeClient, config: Config) {

    private val codec = AerospikeCodec

    private def mkKey(setName: String, key: String): Key = new Key(config.namespace, setName, key)

    private def getRecord(setName: String, keyS: String): IO[Option[Record]] = {
      val key = mkKey(setName, keyS)
      for {
        record <- IO.delay(client.get(config.readPolicy, key))
      } yield Utils.checkForNull(record)
    }

    private def getBytes(setName: String, key: String, binName: String): OptionT[IO, TrackGen[Array[Byte]]]
      = for {
        record <- OptionT(getRecord(setName, key))
        obj <- OptionT.fromOption[IO](Utils.checkForNull(record.bins.get(binName)))
      } yield TrackGen(obj.asInstanceOf[Array[Byte]], record.generation)
    

    private def putBin(setName: String, keyS: String, bin: Bin, generation: Int): IO[Boolean] = IO.delay {
      val key = mkKey(setName, keyS)

      // TODO get policy from client
      val policy = config.writePolicy
      policy.generation = generation
      val operation = Operation.put(bin)
      Try(client.operate(policy, key, operation)).isSuccess
    }

    object Profiles extends KeyValueDB[IO, Cookie, SimpleProfile] {
      def get(cookie: Cookie): IO[TrackGen[SimpleProfile]] = {
        for {
          bytes <- getBytes(config.profilesSetName, cookie.value, config.profileBinName)
          profile <- OptionT.fromOption[IO](bytes.traverse(r => codec.decodeProfile(r)))
        } yield profile
      }.value.map(_.getOrElse(TrackGen.default[SimpleProfile](SimpleProfile.default)))

      def update(cookie: Cookie, profile: TrackGen[SimpleProfile]): IO[Boolean] = {
        val bin = new Bin(config.profileBinName, codec.encodeProfile(profile.value))
        putBin(config.profilesSetName, cookie.value, bin, profile.generation)
      }
    }
    
    object Aggregates extends KeyValueDB[IO, AggregateKey, AggregateValue] {
      def get(key: AggregateKey): IO[TrackGen[AggregateValue]] = {
        for {
          bytes <- getBytes(config.aggregatesSetName, codec.encodeAggregateKey(key), config.aggregateBinName)
          value <- OptionT.fromOption[IO](bytes.traverse(r => codec.decodeAggregateValue(r)))
        } yield value
      }.value.map(_.getOrElse(TrackGen.default(AggregateValue.default)))

      def update(key: AggregateKey, value: TrackGen[AggregateValue]): IO[Boolean] = {
        val bin = new Bin(config.aggregateBinName, codec.encodeAggregateValue(value.value))
        putBin(config.aggregatesSetName, codec.encodeAggregateKey(key), bin, value.generation)
      }  
    }
  }
}
