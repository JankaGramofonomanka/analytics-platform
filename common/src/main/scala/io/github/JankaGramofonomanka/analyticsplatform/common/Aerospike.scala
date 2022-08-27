package io.github.JankaGramofonomanka.analyticsplatform.common

import scala.util.Try
import scala.concurrent.{Future, ExecutionContext}

import cats.effect.IO

import com.aerospike.client.{AerospikeClient, Key, Record, Bin, Operation}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.Config.Environment
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec


object Aerospike {

  final case class Namespace(value: String) extends AnyVal
  final case class BinName  (value: String) extends AnyVal

  class DB(client: AerospikeClient)(implicit env: Environment, ec: ExecutionContext) {

    private object Config {
      val profilesNamespace   = Namespace(env.AEROSPIKE_PROFILES_NAMESPACE)
      val aggregatesNamespace = Namespace(env.AEROSPIKE_AGGREGATES_NAMESPACE)
      val profileBinName      = BinName(env.AEROSPIKE_PROFILES_BIN)
      val aggregateBinName    = BinName(env.AEROSPIKE_AGGREGATES_BIN)
    }

    private val codec = AerospikeCodec

    private def mkKey(namespace: Namespace, key: String): Key = new Key(namespace.value, null, key)

    private def pureFromFuture[A](future: Future[A]): IO[A] = IO.fromFuture { IO.delay { future } }

    
    private def getRecord(namespace: Namespace, keyS: String): IO[TrackGenT[Option, Record]] = {
      val key = mkKey(namespace, keyS)
      for {
        record <- pureFromFuture(Future { client.get(client.readPolicyDefault, key) })
        optRecord = Utils.checkForNull(record)
        generation = optRecord.map(_.generation).getOrElse(Generation.default)
      } yield TrackGenT(TrackGen(optRecord, generation))
    }

    private def getBytes(namespace: Namespace, key: String, binName: BinName): IO[TrackGenT[Option, Array[Byte]]]
      = for {
          record <- getRecord(namespace, key)
      } yield for {
        record <- record
        obj <- Utils.checkForNull(record.getValue(binName.value))
      } yield obj.asInstanceOf[Array[Byte]]


    
    private def putBin(namespace: Namespace, keyS: String, bin: Bin, generation: Int): IO[Boolean] = {
    
      val key = mkKey(namespace, keyS)

      val policy = client.writePolicyDefault
      policy.generation = generation

      for {
        // TODO other exceptions than `GenerationError`?
        res <- pureFromFuture(Future { Try(client.operate(policy, key, Operation.put(bin))) })
        
      } yield res.isSuccess
    }



    object Profiles extends KeyValueDB[IO, Cookie, SimpleProfile] {
      
      def get(cookie: Cookie): IO[TrackGen[SimpleProfile]] = for {
        
        bytes <- getBytes(Config.profilesNamespace, cookie.value, Config.profileBinName)
        decoded = bytes.flatMap(bytes => codec.decodeProfile(bytes)).value

      } yield decoded.map(_.getOrElse(SimpleProfile.default))

      def update(cookie: Cookie, profile: TrackGen[SimpleProfile]): IO[Boolean] = {
        val bin = new Bin(Config.profileBinName.value, codec.encodeProfile(profile.value))
        putBin(Config.profilesNamespace, cookie.value, bin, profile.generation)
      }
    }
    

    
    object Aggregates extends KeyValueDB[IO, AggregateKey, AggregateValue] {

      def get(key: AggregateKey): IO[TrackGen[AggregateValue]] = for {
        
        bytes <- getBytes(Config.aggregatesNamespace, codec.encodeAggregateKey(key), Config.aggregateBinName)
        decoded = bytes.flatMap(bytes => codec.decodeAggregateValue(bytes)).value

      } yield decoded.map(_.getOrElse(AggregateValue.default))

      def update(key: AggregateKey, value: TrackGen[AggregateValue]): IO[Boolean] = {
        val bin = new Bin(Config.aggregateBinName.value, codec.encodeAggregateValue(value.value))
        putBin(Config.aggregatesNamespace, codec.encodeAggregateKey(key), bin, value.generation)
      }  
    }
  }
}

