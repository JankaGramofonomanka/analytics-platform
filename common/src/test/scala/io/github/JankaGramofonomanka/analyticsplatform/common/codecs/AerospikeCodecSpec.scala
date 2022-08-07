package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import org.scalatest.freespec.AnyFreeSpec

//import com.aerospike.client.{Record, Bin}
//import scala.jdk.CollectionConverters._

import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec



class AerospikeCodecSpec extends AnyFreeSpec {
  
  /* TODO figure this out, maybe you need to rewrite `AerospikeCodec` so that 
   * it encodes into the same thing from which it decodes
   */
  
  "\"decode . encode is id\"" - {
    /*
    "`Profile`" in {
        
        val profile = ExampleData.simpleProfile
        val bytes = AerospikeCodec.encodeProfile(profile)
        val decoded = AerospikeCodec.decodeProfile(bytes)

        assert(Some(profile) == decoded)

    }
    */
    "`AggregateValue`" in {

        val value = ExampleData.aggregateValue
        val bytes = AerospikeCodec.encodeAggregateValue(value)
        val decoded = AerospikeCodec.decodeAggregateValue(bytes)

        assert(Some(value) == decoded)
        
    }
  }
}

