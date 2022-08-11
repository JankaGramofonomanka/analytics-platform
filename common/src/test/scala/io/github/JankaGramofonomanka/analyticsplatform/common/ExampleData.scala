package io.github.JankaGramofonomanka.analyticsplatform.common

import java.time.LocalDateTime

import io.circe._
import io.circe.literal._

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._

object ExampleData {

  val cookie    = Cookie("8765128476")
  
  private val bucketStr = "1986-05-23T13:54:00"
  val bucket    = Bucket(LocalDateTime.parse(bucketStr))

  private val timestampStr = "1986-05-23T13:54:39.465"
  private val timestampJsonStr = s"${timestampStr}Z"
  val timestamp = Timestamp(LocalDateTime.parse(timestampStr))
  val from      = LocalDateTime.parse("1986-05-23T13:53:00.000")
  val to        = LocalDateTime.parse("1986-05-23T13:56:00")
  val timeRange = TimeRange(from, to)
  
  val limit       = 50
  val action      = BUY
  val aggregate   = SUM_PRICE
  val device      = PC


  object Brands {
    val reardenSteel = BrandId("Rearden-Steel")
    val wyattOil = BrandId("Wyatt-Oil")
  }

  val origin      = Origin("China")
  val brandId     = Brands.reardenSteel
  val categoryId  = CategoryId("building-materials")
  val country     = Country("USA")
  val price       = Price(200000)
  val productId   = ProductId(563249752)
  
  val productInfo = ProductInfo(productId, brandId, categoryId, price)

  val userTag   = UserTag(timestamp, cookie, country, device, action, origin, productInfo)
  val userTagV  = UserTag(timestamp, cookie, country, device, VIEW,   origin, productInfo)
  val userTagB  = UserTag(timestamp, cookie, country, device, BUY,    origin, productInfo)
  
  val simpleProfile = SimpleProfile(Vector(userTagV, userTagB))
  val prettyProfile = PrettyProfile(cookie, Vector(userTagV), Vector(userTagB))

  val aggregateFields = AggregateFields(action, true, true, Some(origin), Some(brandId), Some(categoryId))

  private val count = 1
  val aggregateValue  = AggregateValue(count, price)

  val aggregateKey    = AggregateKey(bucket, action, Some(origin), Some(brandId), Some(categoryId))
  
  val aggregateItem   = AggregateItem(bucket, aggregateValue)
  val aggregates      = Aggregates(aggregateFields, Vector(aggregateItem))
  


  // Json
  val userTagJson   = jsonTag(userTag .action)
  val userTagJsonV  = jsonTag(userTagV.action)
  val userTagJsonB  = jsonTag(userTagB.action)

  private def jsonTag(action: Action): Json = json"""{
    "time":     ${timestampJsonStr},
    "cookie":   ${cookie.value},
    "country":  ${country.value},
    "device":   ${Device.encode(device)},
    "action":   ${Action.encode(action)},
    "origin":   ${origin.value},
    "product_info": {
      "product_id":   ${productId.value},
      "brand_id":     ${brandId.value},
      "category_id":  ${categoryId.value},
      "price":        ${price.value}
    }
  }"""

  val profileJson = json"""{
    "cookie": ${cookie.value},
    "views":  [${userTagJsonV}],
    "buys":   [${userTagJsonB}]
  }"""

  val aggregatesJson = json"""{
    "columns": ["1m_bucket", "action", "origin", "brand_id", "category_id", "sum_price", "count"],
    "rows": [
      [$bucketStr, ${Action.encode(action)}, ${origin.value}, ${brandId.value}, ${categoryId.value}, ${price.value}, $count]
    ]
  }"""


  object Specification {
    // Examples from the task specification


    val timestampJson1 = json""""2022-03-22T12:15:00.000Z""""
    val timestampJson2 = json""""2022-03-22T12:15:00Z""""
    val timestamp = Timestamp(LocalDateTime.parse("2022-03-22T12:15:00.000"))

    val datetimeJson1 = json""""2022-03-22T12:15:00.000""""
    val datetimeJson2 = json""""2022-03-22T12:15:00""""
    val datetime = Timestamp(LocalDateTime.parse("2022-03-22T12:15:00.000"))

    val timeRangeJson = json""""2022-03-22T12:15:00.000_2022-03-22T12:30:00.000""""
    val timeRange = TimeRange(
      LocalDateTime.parse("2022-03-22T12:15:00.000"),
      LocalDateTime.parse("2022-03-22T12:30:00.000"),
    )


    val aggregatesJson = json"""{
      "columns": ["1m_bucket", "action", "brand_id", "sum_price", "count"],
      "rows": [
        ["2022-03-01T00:05:00", "BUY", "Nike", 1000, 3],
        ["2022-03-01T00:06:00", "BUY", "Nike", 1500, 4],
        ["2022-03-01T00:07:00", "BUY", "Nike", 1200, 2]
      ]
    }"""

    val aggregates = Aggregates(
      AggregateFields(BUY, true, true, None, Some(BrandId("Nike")), None),
      Vector(
        AggregateItem(Bucket(LocalDateTime.parse("2022-03-01T00:05:00")), AggregateValue(3, Price(1000))),
        AggregateItem(Bucket(LocalDateTime.parse("2022-03-01T00:06:00")), AggregateValue(4, Price(1500))),
        AggregateItem(Bucket(LocalDateTime.parse("2022-03-01T00:07:00")), AggregateValue(2, Price(1200))),
      )
    )



  }

}
