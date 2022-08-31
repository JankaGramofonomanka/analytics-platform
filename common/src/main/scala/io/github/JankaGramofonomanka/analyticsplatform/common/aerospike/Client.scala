package io.github.JankaGramofonomanka.analyticsplatform.common.aerospike

import com.aerospike.client.Key

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.aerospike.Data._

trait Client[F[_]] {
  
  def get(key: Key, binName: BinName): F[TrackGenT[Option, Array[Byte]]]

  def put(key: Key, binName: BinName, bytes: Array[Byte], expectedGeneration: Generation): F[Boolean]
}

