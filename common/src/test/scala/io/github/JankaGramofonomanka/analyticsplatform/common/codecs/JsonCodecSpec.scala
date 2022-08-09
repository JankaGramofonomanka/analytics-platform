package io.github.JankaGramofonomanka.analyticsplatform.common.codecs

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.Assertion

import io.circe._
import io.circe.syntax._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.codecs.JsonCodec._




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
    "`Cookie`"          in testJson[Cookie]         (ExampleData.cookie)
    "`Timestamp`"       in testJson[Timestamp]      (ExampleData.timestamp)
    "`TimeRange`"       in testJson[TimeRange]      (ExampleData.timeRange)
    "`Action`"          in testJson[Action]         (ExampleData.action)
    "`Aggregate`"       in testJson[Aggregate]      (ExampleData.aggregate)
    "`Device`"          in testJson[Device]         (ExampleData.device)
    "`Origin`"          in testJson[Origin]         (ExampleData.origin)
    "`BrandId`"         in testJson[BrandId]        (ExampleData.brandId)
    "`CategoryId`"      in testJson[CategoryId]     (ExampleData.categoryId)
    "`Country`"         in testJson[Country]        (ExampleData.country)
    "`Price`"           in testJson[Price]          (ExampleData.price)
    "`ProductId`"       in testJson[ProductId]      (ExampleData.productId)
    "`ProductInfo`"     in testJson[ProductInfo]    (ExampleData.productInfo)
    "`UserTag`"         in testJson[UserTag]        (ExampleData.userTag)
    "`SimpleProfile`"   in testJson[SimpleProfile]  (ExampleData.simpleProfile)
    "`PrettyProfile`"   in testJson[PrettyProfile]  (ExampleData.prettyProfile)
    "`AggregateKey`"    in testJson[AggregateKey]   (ExampleData.aggregateKey)
    "`AggregateValue`"  in testJson[AggregateValue] (ExampleData.aggregateValue)
  }

  "encoding" - {
    "`UserTag`"       in testEncoding[UserTag]      (ExampleData.userTag,       ExampleData.userTagJson)
    "`PrettyProfile`" in testEncoding[PrettyProfile](ExampleData.prettyProfile, ExampleData.profileJson)
    "`Aggregates`"    in testEncoding[Aggregates]   (ExampleData.aggregates,    ExampleData.aggregatesJson)
    
    "examples from specification" - {
      val Examples = ExampleData.Specification
      "timestamp"   in testEncoding[Timestamp]  (Examples.timestamp,   Examples.timestampJson)
      "time range"  in testEncoding[TimeRange]  (Examples.timeRange,   Examples.timeRangeJson)
      "aggregates"  in testEncoding[Aggregates] (Examples.aggregates,  Examples.aggregatesJson)
    }

  }

  "decoding" - {
    "`UserTag`" in testDecoding(ExampleData.userTagJson, ExampleData.userTag)
    "examples from specification" - {
      val Examples = ExampleData.Specification
      "timestamp"   in testDecoding[Timestamp](Examples.timestampJson, Examples.timestamp)
      "time range"  in testDecoding[TimeRange](Examples.timeRangeJson, Examples.timeRange)
    }
    

  }


}

