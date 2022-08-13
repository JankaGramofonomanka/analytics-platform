package io.github.JankaGramofonomanka.analyticsplatform.common

import scala.util.Try

import cats.data.OptionT
import cats.effect.IO

import com.aerospike.client.{AerospikeClient, Key, Record, Bin, Operation}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec


object Aerospike {

  final case class Namespace(value: String) extends AnyVal
  final case class SetName  (value: String) extends AnyVal
  final case class BinName  (value: String) extends AnyVal

  // TODO replace this with `common.Config.Aerospike`
  final case class Config(
    namespace:          Namespace,
    profilesSetName:    SetName,
    aggregatesSetName:  SetName,
    profileBinName:     BinName,
    aggregateBinName:   BinName,
  )
  
  

  class DB(client: AerospikeClient, config: Config) {

    private val codec = AerospikeCodec

    private def mkKey(setName: SetName, key: String): Key = new Key(config.namespace.value, setName.value, key)

    private def getRecord(setName: SetName, keyS: String): IO[Option[Record]] = {
      val key = mkKey(setName, keyS)
      for {
        record <- IO.delay(client.get(client.readPolicyDefault, key))
      } yield Utils.checkForNull(record)
    }

    private def getBytes(setName: SetName, key: String, binName: BinName): OptionT[IO, TrackGen[Array[Byte]]]
      = for {
        record <- OptionT(getRecord(setName, key))
        obj <- OptionT.fromOption[IO](Utils.checkForNull(record.bins.get(binName)))
      } yield TrackGen(obj.asInstanceOf[Array[Byte]], record.generation)
    

    private def putBin(setName: SetName, keyS: String, bin: Bin, generation: Int): IO[Boolean] = IO.delay {
      val key = mkKey(setName, keyS)

      val policy = client.writePolicyDefault
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
        val bin = new Bin(config.profileBinName.value, codec.encodeProfile(profile.value))
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
        val bin = new Bin(config.aggregateBinName.value, codec.encodeAggregateValue(value.value))
        putBin(config.aggregatesSetName, codec.encodeAggregateKey(key), bin, value.generation)
      }  
    }
  }
}

