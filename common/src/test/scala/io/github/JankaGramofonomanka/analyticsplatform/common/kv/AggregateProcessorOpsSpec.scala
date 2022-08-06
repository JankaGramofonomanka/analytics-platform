package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import org.scalatest.freespec.AnyFreeSpec

import cats.effect._
import cats.effect.unsafe.implicits.global

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.OpsSpecUtils._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.TestCaseData._


class AggregateProcessorOpsSpec extends AnyFreeSpec {

  "`AggregateProcessorOps.processTags`" - {
    
    val storage = Storage.empty
    val interface = getMocks[IO](storage)

    val (_, proc) = getOps[IO](interface)

    storage.queue.addOne(Case1.tagRS)
    storage.queue.addOne(Case1.tagWO1)
    storage.queue.addOne(Case1.tagWO2)

    proc.processTags.take(3).compile.drain.unsafeRunSync()


    val infoAll = AggregateInfo(Case1.bucket, Case1.action, None, None, None)
    val infoRS = infoAll.copy(brandId = Some(ExampleData.Brands.reardenSteel))
    val infoWO = infoAll.copy(brandId = Some(ExampleData.Brands.wyattOil))
    
    
    "correctly processes one tag" in {
      val result = storage.aggregates.get(infoRS)
      assert(result == Some(Case1.expectedAggregateValueRS))
    }
    
    "correctly processes two tags" in {
      val result = storage.aggregates.get(infoWO)
      assert(result == Some(Case1.expectedAggregateValueWO))
    }
    
    "correctly processes tags with differend field values" in {
      val result = storage.aggregates.get(infoAll)
      assert(result == Some(Case1.expectedAggregateValueAll))
    }  
  }
  
}
