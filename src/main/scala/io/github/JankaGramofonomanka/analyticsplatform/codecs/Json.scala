package io.github.JankaGramofonomanka.analyticsplatform.codecs

import cats.effect.IO

import org.http4s._
import io.circe.generic.auto._

import io.github.JankaGramofonomanka.analyticsplatform.Data._

abstract class JsonCodec[F[_]] {
  implicit val productInfoDecoder: EntityDecoder[F, ProductInfo]
  implicit val userTagDecoder: EntityDecoder[F, UserTag]
  
  implicit val profileEncoder: EntityEncoder[F, PrettyProfile]
  implicit val aggregatesEncoder: EntityEncoder[F, Aggregates]
}


object IOJsonCodec extends JsonCodec[IO] {

  // TODO define custom decoders
  implicit val productInfoDecoder = org.http4s.circe.jsonOf[IO, ProductInfo]
  implicit val userTagDecoder = org.http4s.circe.jsonOf[IO, UserTag]

  // TODO define encoders
  implicit val profileEncoder: EntityEncoder[IO, PrettyProfile] = EntityEncoder.emptyEncoder
  implicit val aggregatesEncoder: EntityEncoder[IO, Aggregates] = EntityEncoder.emptyEncoder
}


