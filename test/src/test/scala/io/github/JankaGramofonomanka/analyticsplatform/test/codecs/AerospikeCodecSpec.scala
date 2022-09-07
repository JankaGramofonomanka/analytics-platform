package io.github.JankaGramofonomanka.analyticsplatform.test.codecs

import org.scalatest.freespec.AnyFreeSpec

import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.AerospikeCodec
import io.github.JankaGramofonomanka.analyticsplatform.test.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.test.TestUtils


class AerospikeCodecSpec extends AnyFreeSpec {

  implicit val env = TestUtils.MockEnv

  val codec = AerospikeCodec
  
  "\"decode . encode is id\"" - {
    "`Profile`" in {
        
        val profile = ExampleData.General.profile
        val bytes = codec.encodeProfile(profile)
        val decoded = codec.decodeProfile(bytes)

        assert(Some(profile) == decoded)

    }
    "`AggregateValue`" in {

        val value = ExampleData.General.aggregateVB
        val bytes = codec.encodeAggregateVB(value)
        val decoded = codec.decodeAggregateVB(bytes)

        assert(Some(value) == decoded)
        
    }
  }
}

