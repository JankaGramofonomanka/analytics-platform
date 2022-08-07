package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import org.scalatest.freespec.AnyFreeSpec

import cats.effect._
import cats.effect.unsafe.implicits.global

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


    "correctly processes one tag" in {
      val result = storage.aggregates.get(Case1.keyRS)
      assert(result == Some(Case1.expectedAggregateValueRS))
    }
    
    "correctly processes two tags" in {
      val result = storage.aggregates.get(Case1.keyWO)
      assert(result == Some(Case1.expectedAggregateValueWO))
    }
    
    "correctly processes tags with differend field values" in {
      val result = storage.aggregates.get(Case1.keyAll)
      assert(result == Some(Case1.expectedAggregateValueAll))
    }  
  }
  
}
