package io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs

import io.circe.syntax._

import org.apache.kafka.common.serialization._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._

object KafkaCodec {
  
  class UserTagSerializer extends Serializer[UserTag] {

    val stringSerializer = new StringSerializer

    def serialize(topic: String, data: UserTag): Array[Byte] = {

      // TODO use compression etc.
      val asString = data.asJson.noSpaces
      stringSerializer.serialize(topic, asString)
    }
  }

}
