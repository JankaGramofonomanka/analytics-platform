package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import org.scalatest.freespec.AnyFreeSpec

import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.KafkaCodec._



class KafkaCodecSpec extends AnyFreeSpec {

  "decode . encode is id" in {

    val ser = new UserTagSerializer
    val des = new UserTagDeserializer

    val tag = ExampleData.userTag

    val topic = "topic"

    val serialized    = ser.serialize(topic, tag)
    val deserialized  = des.deserialize(topic, serialized)

    assert(deserialized == tag)

  }
}

