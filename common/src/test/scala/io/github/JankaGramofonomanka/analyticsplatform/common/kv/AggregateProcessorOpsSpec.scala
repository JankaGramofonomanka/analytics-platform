package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import org.scalatest.freespec.AnyFreeSpec

import cats.effect._
import cats.effect.unsafe.implicits.global

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.OpsSpecUtils._



class AggregateProcessorOpsSpec extends AnyFreeSpec {

  "`AggregateProcessorOps.processTags`" - {
    
    val storage = Storage.empty
    val interface = getMocks[IO](storage)

    val (_, proc) = getOps[IO](interface)

    val priceRS = Price(100)
    val priceWO1 = Price(100)
    val priceWO2 = Price(200)

    val productInfoRS   = ExampleData.productInfo.copy(brandId = ExampleData.Brands.reardenSteel, price = priceRS)
    val productInfoWO1  = ExampleData.productInfo.copy(brandId = ExampleData.Brands.wyattOil,     price = priceWO1)
    val productInfoWO2  = ExampleData.productInfo.copy(brandId = ExampleData.Brands.wyattOil,     price = priceWO2)

    val tagRS = ExampleData.userTag.copy(productInfo = productInfoRS)
    val tagWO1 = ExampleData.userTag.copy(productInfo = productInfoWO1)
    val tagWO2 = ExampleData.userTag.copy(productInfo = productInfoWO2)

    
    storage.queue.addOne(tagRS)
    storage.queue.addOne(tagWO1)
    storage.queue.addOne(tagWO2)

    proc.processTags.take(3).compile.drain.unsafeRunSync()


    val bucket = tagRS.time.getBucket
    val action = tagRS.action

    val infoNone = AggregateInfo(bucket, action, None, None, None)
    val infoRS = infoNone.copy(brandId = Some(ExampleData.Brands.reardenSteel))
    val infoWO = infoNone.copy(brandId = Some(ExampleData.Brands.wyattOil))
    
    
    "correctly processes one tag" in {
      val result = storage.aggregates.get(infoRS)
      println(s"RS: $result")
      assert(result == Some(AggregateValue(1, priceRS)))
    }
    
    "correctly processes two tags" in {
      val result = storage.aggregates.get(infoWO)
      println(s"WO: $result")
      assert(result == Some(AggregateValue(2, priceWO1 + priceWO2)))
    }
    
    "correctly processes tags with differend field values" in {
      val result = storage.aggregates.get(infoNone)
      println(s"all: $result")
      assert(result == Some(AggregateValue(3, priceRS + priceWO1 + priceWO2)))
    }  
  }
  
}
