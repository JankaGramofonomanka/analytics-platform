package io.github.JankaGramofonomanka.analyticsplatform.tagprocessor.codecs

import io.circe.parser._

import org.apache.kafka.common.serialization._
import org.apache.kafka.common.errors.SerializationException

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._

object KafkaCodec {
  
  class UserTagDeserializer extends Deserializer[UserTag] {
    val stringDeserializer = new StringDeserializer

    def deserialize(topic: String, data: Array[Byte]): UserTag = {

      // TODO use compression etc.
      val asString = stringDeserializer.deserialize(topic, data)

      // TODO this is horrible, find a better way
      decode[UserTag](asString) match {
        case Right(result) => result
        case Left(err) => throw new SerializationException(err.getMessage)
      }
    }
  }


}
