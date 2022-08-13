package io.github.JankaGramofonomanka.analyticsplatform.test.codecs

import org.scalatest.freespec.AnyFreeSpec


import io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs.KafkaCodec.UserTagSerializer
import io.github.JankaGramofonomanka.analyticsplatform.aggregateprocessor.codecs.KafkaCodec.UserTagDeserializer
import io.github.JankaGramofonomanka.analyticsplatform.test.ExampleData



class KafkaCodecSpec extends AnyFreeSpec {

  "decode . encode is id" in {

    val ser = new UserTagSerializer
    val des = new UserTagDeserializer

    val tag = ExampleData.General.userTag

    val topic = "topic"

    val serialized    = ser.serialize(topic, tag)
    val deserialized  = des.deserialize(topic, serialized)

    assert(deserialized == tag)

  }
}

