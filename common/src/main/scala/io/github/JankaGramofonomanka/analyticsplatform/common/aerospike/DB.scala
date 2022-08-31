package io.github.JankaGramofonomanka.analyticsplatform.common.aerospike

import cats.effect.IO

import com.aerospike.client.Key

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Config.Environment
import io.github.JankaGramofonomanka.analyticsplatform.common.KeyValueDB
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.Client
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.Data._


class DB(client: Client[IO])(implicit env: Environment) {

  private object Config {
    val profilesNamespace   = Namespace(env.AEROSPIKE_PROFILES_NAMESPACE)
    val aggregatesNamespace = Namespace(env.AEROSPIKE_AGGREGATES_NAMESPACE)
    val profileBinName      = BinName(env.AEROSPIKE_PROFILES_BIN)
  }

  private val codec = AerospikeCodec

  private def mkKey(namespace: Namespace, key: String): Key = {
    new Key(namespace.value, null, key)
  }

  private def keyAndBin(key: AggregateKey): (AggregateKey, BinName) = {
    val actualKey = key.getBatchKey(env.AEROSPIKE_BUCKETS_PER_KEY)
    val offset = key.getBatchOffset(env.AEROSPIKE_BUCKETS_PER_KEY)
    val binName = BinName(offset.toString)

    (actualKey, binName)
  }

  object Profiles extends KeyValueDB[IO, Cookie, Profile] {
    
    def get(cookie: Cookie): IO[TrackGen[Profile]] = {
      val key = mkKey(Config.profilesNamespace, cookie.value)
      for {
        bytes <- client.get(key, Config.profileBinName)
        decoded = bytes.flatMap(bytes => codec.decodeProfile(bytes)).value
      } yield decoded.map(_.getOrElse(Profile.default(cookie)))
    }

    def update(cookie: Cookie, profile: TrackGen[Profile]): IO[Boolean] = {
      val key = mkKey(Config.profilesNamespace, cookie.value)
      client.put(key, Config.profileBinName, codec.encodeProfile(profile.value), profile.generation)
    }
  }
  
  object Aggregates extends KeyValueDB[IO, AggregateKey, AggregateVB] {

    def get(key: AggregateKey): IO[TrackGen[AggregateVB]] = {
      
      val (actualKey, binName) = keyAndBin(key)

      val k = mkKey(Config.aggregatesNamespace, codec.encodeAggregateKey(actualKey))
      for {
        
        bytes <- client.get(k, binName)
        decoded = bytes.flatMap(bytes => codec.decodeAggregateVB(bytes)).value

      } yield decoded.map(_.getOrElse(AggregateVB.default))
    }

    def update(key: AggregateKey, vb: TrackGen[AggregateVB]): IO[Boolean] = {
      val (actualKey, binName) = keyAndBin(key)

      val k = mkKey(Config.aggregatesNamespace, codec.encodeAggregateKey(actualKey))
      client.put(k, binName, codec.encodeAggregateVB(vb.value), vb.generation)
    }  
  }
}


