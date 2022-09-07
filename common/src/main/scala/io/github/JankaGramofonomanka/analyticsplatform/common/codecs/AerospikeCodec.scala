package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import io.circe._
import io.circe.syntax._
import io.circe.parser._

import org.xerial.snappy.Snappy.{compress, uncompress}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._


object AerospikeCodec {


  private def decodeJson[A: Decoder](bytes: Array[Byte]): Option[A] = {
    val str = new String(uncompress(bytes))
    // TODO propagate the error?
    decode[A](str).toOption
  }

  def encodeProfile(profile: Profile): Array[Byte] = compress(profile.asJson.noSpaces)

  def decodeProfile(bytes: Array[Byte]): Option[Profile]
    = decodeJson[Profile](bytes)

  def encodeAggregateVB(aggregateVB: AggregateVB): Array[Byte]
    = compress(aggregateVB.asJson.noSpaces)
  
  def decodeAggregateVB(bytes: Array[Byte]): Option[AggregateVB]
    = decodeJson[AggregateVB](bytes)
  
  def encodeAggregateKey(key: AggregateKey): String = key.asJson.noSpaces


}

