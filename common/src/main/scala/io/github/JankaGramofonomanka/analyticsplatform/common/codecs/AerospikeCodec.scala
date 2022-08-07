package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import io.circe._
import io.circe.syntax._
import io.circe.parser._

import com.aerospike.client.{Record, Bin}

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils._


// TODO consider moving bin names somewhere
class AerospikeCodec(profileBinName: String, aggregateBinName: String) {

  private def decodeRecord[A: Decoder](record: Record, binName: String): Option[A] = {
    val obj = checkForNull(record.bins.get(binName))
    
    obj.flatMap {
      
      obj => {
      
        val bytes = obj.asInstanceOf[Array[Byte]]
        val str = new String(bytes)

        // TODO propagate the error?
        decode[A](str).toOption

      }
    }
  }

  def decodeProfile(record: Record): Option[SimpleProfile]
    = decodeRecord[SimpleProfile](record, profileBinName)

  def encodeProfile(profile: SimpleProfile): Bin = {
    
    val value: Array[Byte] = profile.asJson.noSpaces.toString.getBytes
    new Bin(profileBinName, value)
  }

  def encodeAggregateKey(key: AggregateKey): String = key.asJson.noSpaces.toString

  def encodeAggregateValue(aggregateValue: AggregateValue): Bin = {
    val value: Array[Byte] = aggregateValue.asJson.noSpaces.toString.getBytes
    new Bin(aggregateBinName, value)
  }
  
  def decodeAggregateValue(record: Record): Option[AggregateValue]
    = decodeRecord[AggregateValue](record, aggregateBinName)


}
