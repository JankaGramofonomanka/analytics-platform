package io.github.JankaGramofonomanka.analyticsplatform.common

import scala.util.Try

import cats.effect.IO

import com.aerospike.client.{AerospikeClient, Key, Record, Bin, Operation}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.Config.Environment
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec


object Aerospike {

  final case class Namespace(value: String) extends AnyVal
  final case class SetName  (value: String) extends AnyVal
  final case class BinName  (value: String) extends AnyVal

  class DB(client: AerospikeClient)(implicit env: Environment) {

    private object Config {
      val namespace         = Namespace(env.AEROSPIKE_NAMESPACE)
      val profilesSetName   = SetName(env.AEROSPIKE_PROFILES_SET)
      val aggregatesSetName = SetName(env.AEROSPIKE_AGGREGATES_SET)
      val profileBinName    = BinName(env.AEROSPIKE_PROFILES_BIN)
      val aggregateBinName  = BinName(env.AEROSPIKE_AGGREGATES_BIN)
    }

    private val codec = AerospikeCodec

    private def mkKey(setName: SetName, key: String): Key = new Key(Config.namespace.value, setName.value, key)

    
    private def getRecord(setName: SetName, keyS: String): IO[TrackGenT[Option, Record]] = {
      val key = mkKey(setName, keyS)
      for {
        record <- IO.delay(client.get(client.readPolicyDefault, key))
        optRecord = Utils.checkForNull(record)
        generation = optRecord.map(_.generation).getOrElse(Generation.default)
      } yield TrackGenT(TrackGen(optRecord, generation))
    }

    private def getBytes(setName: SetName, key: String, binName: BinName): IO[TrackGenT[Option, Array[Byte]]]
      = for {
          record <- getRecord(setName, key)
      } yield for {
        record <- record
        obj <- Utils.checkForNull(record.getValue(binName.value))
      } yield obj.asInstanceOf[Array[Byte]]


    private def putBin(setName: SetName, keyS: String, bin: Bin, generation: Int): IO[Boolean] = IO.delay {
      val key = mkKey(setName, keyS)

      val policy = client.writePolicyDefault
      policy.generation = generation

      Try(client.operate(policy, key, Operation.put(bin))).isSuccess
    }



    object Profiles extends KeyValueDB[IO, Cookie, SimpleProfile] {
      
      def get(cookie: Cookie): IO[TrackGen[SimpleProfile]] = for {
        
        bytes <- getBytes(Config.profilesSetName, cookie.value, Config.profileBinName)
        decoded = bytes.flatMap(bytes => codec.decodeProfile(bytes)).value

      } yield decoded.map(_.getOrElse(SimpleProfile.default))

      def update(cookie: Cookie, profile: TrackGen[SimpleProfile]): IO[Boolean] = {
        val bin = new Bin(Config.profileBinName.value, codec.encodeProfile(profile.value))
        putBin(Config.profilesSetName, cookie.value, bin, profile.generation)
      }
    }
    

    
    object Aggregates extends KeyValueDB[IO, AggregateKey, AggregateValue] {

      def get(key: AggregateKey): IO[TrackGen[AggregateValue]] = for {
        
        bytes <- getBytes(Config.aggregatesSetName, codec.encodeAggregateKey(key), Config.aggregateBinName)
        decoded = bytes.flatMap(bytes => codec.decodeAggregateValue(bytes)).value

      } yield decoded.map(_.getOrElse(AggregateValue.default))

      def update(key: AggregateKey, value: TrackGen[AggregateValue]): IO[Boolean] = {
        val bin = new Bin(Config.aggregateBinName.value, codec.encodeAggregateValue(value.value))
        putBin(Config.aggregatesSetName, codec.encodeAggregateKey(key), bin, value.generation)
      }  
    }
  }
}

