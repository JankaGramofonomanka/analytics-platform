package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import cats.effect.IO

import org.http4s.{EntityEncoder, EntityDecoder}
import io.circe._
import io.circe.syntax._



import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec

abstract class EntityCodec[F[_]] {
  implicit val productInfoDecoder: EntityDecoder[F, ProductInfo]
  implicit val userTagDecoder: EntityDecoder[F, UserTag]
  
  implicit val profileEncoder: EntityEncoder[F, PrettyProfile]
  implicit val aggregatesEncoder: EntityEncoder[F, Aggregates]
}


object IOEntityCodec extends EntityCodec[IO] {

  implicit val productInfoJsonDecoder:  Decoder[ProductInfo]    = JsonCodec.productInfoDecoder
  implicit val userTagJsonDecoder:      Decoder[UserTag]        = JsonCodec.userTagDecoder

  implicit val profileJsonEncoder:      Encoder[PrettyProfile]  = JsonCodec.prettyProfileEncoder
  implicit val aggregatesJsonEncoder:   Encoder[Aggregates]     = JsonCodec.aggregatesEncoder

  implicit val productInfoDecoder = org.http4s.circe.jsonOf[IO, ProductInfo]
  implicit val userTagDecoder = org.http4s.circe.jsonOf[IO, UserTag]

  implicit val profileEncoder: EntityEncoder[IO, PrettyProfile] = EntityEncoder.stringEncoder[IO]
    .contramap { profile: PrettyProfile => profile.asJson.toString }
  implicit val aggregatesEncoder: EntityEncoder[IO, Aggregates] = EntityEncoder.stringEncoder[IO]
    .contramap { aggregates: Aggregates => aggregates.asJson.toString }
}




