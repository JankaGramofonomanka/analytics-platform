package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import io.circe._
import io.circe.syntax._
import io.circe.parser._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._


object AerospikeCodec {

  private def decodeJson[A: Decoder](bytes: Array[Byte]): Option[A]
    // TODO propagate the error?
    = decode[A](new String(bytes)).toOption

  def decodeProfile(bytes: Array[Byte]): Option[SimpleProfile]
    = decodeJson[SimpleProfile](bytes)

  def encodeProfile(profile: SimpleProfile): Array[Byte] = profile.asJson.noSpaces.getBytes

  def encodeAggregateKey(key: AggregateKey): String = key.asJson.noSpaces

  def encodeAggregateValue(aggregateValue: AggregateValue): Array[Byte]
    = aggregateValue.asJson.noSpaces.getBytes
  
  def decodeAggregateValue(bytes: Array[Byte]): Option[AggregateValue]
    = decodeJson[AggregateValue](bytes)
    



}

