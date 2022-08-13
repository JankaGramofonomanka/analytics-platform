package io.github.JankaGramofonomanka.analyticsplatform.test.codecs

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.Assertion

import io.circe._
import io.circe.syntax._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._
import io.github.JankaGramofonomanka.analyticsplatform.test.ExampleData._




class JsonCodecSpec extends AnyFreeSpec {

  private def decodeJson[T: Decoder](json: Json): Decoder.Result[T]
    = implicitly[Decoder[T]].decodeJson(json)

  private def testJson[A](a: A)(implicit encoder: Encoder[A], decoder: Decoder[A]): Assertion = {
    val json = encoder(a)
    val decoded = decoder.decodeJson(json)
    assert(Right(a) == decoded)
  }

  private def testEncoding[T: Encoder](toEncode: T, expected: Json): Assertion = {
    val actual  = toEncode.asJson
    assert(expected == actual)
  }

  private def testDecoding[T: Decoder](toDecode: Json, expected: T): Assertion = {
    val actual = decodeJson[T](toDecode)
    assert(Right(expected) == actual)
  }

  "decode . encode is id" - {
    "`Cookie`"          in testJson[Cookie]         (General.cookie)
    "`Timestamp`"       in testJson[Timestamp]      (General.timestamp)
    "`TimeRange`"       in testJson[TimeRange]      (General.timeRange)
    "`Action`"          in testJson[Action]         (General.action)
    "`Aggregate`"       in testJson[Aggregate]      (General.aggregate)
    "`Device`"          in testJson[Device]         (General.device)
    "`Origin`"          in testJson[Origin]         (General.origin)
    "`BrandId`"         in testJson[BrandId]        (General.brandId)
    "`CategoryId`"      in testJson[CategoryId]     (General.categoryId)
    "`Country`"         in testJson[Country]        (General.country)
    "`Price`"           in testJson[Price]          (General.price)
    "`ProductId`"       in testJson[ProductId]      (General.productId)
    "`ProductInfo`"     in testJson[ProductInfo]    (General.productInfo)
    "`UserTag`"         in testJson[UserTag]        (General.userTag)
    "`SimpleProfile`"   in testJson[SimpleProfile]  (General.simpleProfile)
    "`PrettyProfile`"   in testJson[PrettyProfile]  (General.prettyProfile)
    "`AggregateKey`"    in testJson[AggregateKey]   (General.aggregateKey)
    "`AggregateValue`"  in testJson[AggregateValue] (General.aggregateValue)
  }

  "encoding" - {
    "`UserTag`"       in testEncoding[UserTag]      (General.userTag,       General.userTagJson)
    "`PrettyProfile`" in testEncoding[PrettyProfile](General.prettyProfile, General.profileJson)
    "`Aggregates`"    in testEncoding[Aggregates]   (General.aggregates,    General.aggregatesJson)
    
    "examples from specification" - {
      "timestamp"   in testEncoding[Timestamp]  (Specification.timestamp,   Specification.timestampJson1)
      "time range"  in testEncoding[TimeRange]  (Specification.timeRange,   Specification.timeRangeJson)
      "aggregates"  in testEncoding[Aggregates] (Specification.aggregates,  Specification.aggregatesJson)
    }

  }

  "decoding" - {
    "`UserTag`" in testDecoding(General.userTagJson, General.userTag)
    "examples from specification" - {
      "timestamp"   in {
        testDecoding[Timestamp](Specification.timestampJson1, Specification.timestamp)
        testDecoding[Timestamp](Specification.timestampJson2, Specification.timestamp)
      }
      "time range"  in testDecoding[TimeRange](Specification.timeRangeJson, Specification.timeRange)
    }
    

  }


}

