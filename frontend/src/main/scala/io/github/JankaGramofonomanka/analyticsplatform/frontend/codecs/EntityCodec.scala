package io.github.JankaGramofonomanka.analyticsplatform.frontend.codecs

import cats.effect.IO

import org.http4s.{EntityEncoder, EntityDecoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}
import io.circe._


import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec

abstract class EntityCodec[F[_]] {
  implicit val productInfoDecoder: EntityDecoder[F, ProductInfo]
  implicit val userTagDecoder: EntityDecoder[F, UserTag]
  
  implicit val profileEntityEncoder: EntityEncoder[F, Profile]
  implicit val aggregatesEntityEncoder: EntityEncoder[F, Aggregates]

}


object IOEntityCodec extends EntityCodec[IO] {

  implicit val productInfoJsonDecoder:  Decoder[ProductInfo]  = JsonCodec.productInfoDecoder
  implicit val userTagJsonDecoder:      Decoder[UserTag]      = JsonCodec.userTagDecoder

  implicit val profileJsonEncoder:      Encoder[Profile]      = JsonCodec.profileEncoder
  implicit val aggregatesJsonEncoder:   Encoder[Aggregates]   = JsonCodec.aggregatesEncoder

  implicit val productInfoDecoder:  EntityDecoder[IO, ProductInfo] = jsonOf[IO, ProductInfo]
  implicit val userTagDecoder:      EntityDecoder[IO, UserTag]     = jsonOf[IO, UserTag]

  implicit val profileEntityEncoder:    EntityEncoder[IO, Profile]    = jsonEncoderOf[IO, Profile]
  implicit val aggregatesEntityEncoder: EntityEncoder[IO, Aggregates] = jsonEncoderOf[IO, Aggregates]

}




