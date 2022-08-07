package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.OpsSpecUtils._

object TestCaseData {
  object Case1 {
    val priceRS = Price(100)
    val priceWO1 = Price(100)
    val priceWO2 = Price(200)

    val brandRS = ExampleData.Brands.reardenSteel
    val brandWO = ExampleData.Brands.wyattOil

    val productInfoRS   = ExampleData.productInfo.copy(brandId = brandRS, price = priceRS)
    val productInfoWO1  = ExampleData.productInfo.copy(brandId = brandWO, price = priceWO1)
    val productInfoWO2  = ExampleData.productInfo.copy(brandId = brandWO, price = priceWO2)

    val tagRS = ExampleData.userTag.copy(productInfo = productInfoRS)
    val tagWO1 = ExampleData.userTag.copy(productInfo = productInfoWO1)
    val tagWO2 = ExampleData.userTag.copy(productInfo = productInfoWO2)

    val cookie = tagRS.cookie
    val bucket = tagRS.time.getBucket
    val action = tagRS.action

    val infoAll = AggregateInfo(bucket, action, None, None, None)
    val infoRS = infoAll.copy(brandId = Some(ExampleData.Brands.reardenSteel))
    val infoWO = infoAll.copy(brandId = Some(ExampleData.Brands.wyattOil))

    val fieldsAll = AggregateFields(action, true, true, None, None, None)
    val fieldsRS  = fieldsAll.copy(brandId = Some(Case1.brandRS))
    val fieldsWO  = fieldsAll.copy(brandId = Some(Case1.brandWO))
    

    val timeRange = getTimeRangeContaining(bucket.toTimestamp)

    val expectedAggregateValueAll = AggregateValue(3, priceRS + priceWO1 + priceWO2)
    val expectedAggregateValueRS = AggregateValue(1, priceRS)
    val expectedAggregateValueWO = AggregateValue(2, priceWO1 + priceWO2)
    
  }

  object Case2 {
    val cookie1 = Cookie("1")
    val cookie2 = Cookie("2")

    val tag1 = ExampleData.userTag.copy(cookie = cookie1)
    val tag2 = ExampleData.userTag.copy(cookie = cookie2)

    
    val timeRange = getTimeRangeContaining(tag1.time)
    
  }
}