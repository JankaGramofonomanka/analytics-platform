package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import org.scalatest.freespec.AnyFreeSpec

import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.TestUtils
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec


class AerospikeCodecSpec extends AnyFreeSpec {

  implicit val env = TestUtils.mockEnv

  val codec = AerospikeCodec
  
  "\"decode . encode is id\"" - {
    "`Profile`" in {
        
        val profile = ExampleData.simpleProfile
        val bytes = codec.encodeProfile(profile)
        val decoded = codec.decodeProfile(bytes)

        assert(Some(profile) == decoded)

    }
    "`AggregateValue`" in {

        val value = ExampleData.aggregateValue
        val bytes = codec.encodeAggregateValue(value)
        val decoded = codec.decodeAggregateValue(bytes)

        assert(Some(value) == decoded)
        
    }
  }
}

