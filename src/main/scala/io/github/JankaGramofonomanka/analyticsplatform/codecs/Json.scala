package io.github.JankaGramofonomanka.analyticsplatform.codecs

import org.http4s._

import io.github.JankaGramofonomanka.analyticsplatform.Data._

abstract class JsonCodec[F[_]] {
  implicit val productInfoDecoder: EntityDecoder[F, ProductInfo]
  implicit val userTagDecoder: EntityDecoder[F, UserTag]
  
  implicit val profileEncoder: EntityEncoder[F, PrettyProfile]
  implicit val aggregatesEncoder: EntityEncoder[F, Aggregates]
}


