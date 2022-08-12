package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import io.circe._
import io.circe.syntax._
import io.circe.parser._

import org.xerial.snappy.Snappy.{compress, uncompress}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._


object AerospikeCodec {

  private def decodeJson[A: Decoder](bytes: Array[Byte]): Option[A] = {
    // TODO propagate the error?
    val str = new String(uncompress(bytes))
    decode[A](str).toOption
  }

  def encodeProfile(profile: SimpleProfile): Array[Byte] = compress(profile.asJson.noSpaces)

  def decodeProfile(bytes: Array[Byte]): Option[SimpleProfile]
    = decodeJson[SimpleProfile](bytes)

  def encodeAggregateValue(aggregateValue: AggregateValue): Array[Byte]
    = compress(aggregateValue.asJson.noSpaces)
  
  def decodeAggregateValue(bytes: Array[Byte]): Option[AggregateValue]
    = decodeJson[AggregateValue](bytes)
  
  def encodeAggregateKey(key: AggregateKey): String = key.asJson.noSpaces


}

