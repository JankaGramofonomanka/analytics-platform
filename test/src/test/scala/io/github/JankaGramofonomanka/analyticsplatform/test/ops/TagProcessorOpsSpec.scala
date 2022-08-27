package io.github.JankaGramofonomanka.analyticsplatform.test.ops

import org.scalatest.freespec.AnyFreeSpec

import cats.effect._
import cats.effect.unsafe.implicits.global


import io.github.JankaGramofonomanka.analyticsplatform.test.ExampleData.Case1
import io.github.JankaGramofonomanka.analyticsplatform.test.TestUtils


class TagProcessorOpsSpec extends AnyFreeSpec {

  "`TagProcessorOps.processTags`" - {
    
    val storage = TestUtils.Storage.empty
    val interface = TestUtils.getMocks(storage)

    val (_, proc) = TestUtils.getOps[IO](interface)

    storage.queue.addOne(Case1.tagRS)
    storage.queue.addOne(Case1.tagWO1)
    storage.queue.addOne(Case1.tagWO2)

    proc.processTags.take(3).compile.drain.unsafeRunSync()


    "correctly processes one tag" in {
      val result = storage.aggregates.get(Case1.keyRS).map(_.value)
      assert(result == Some(Case1.expectedAggregateValueRS))
    }
    
    "correctly processes two tags" in {
      val result = storage.aggregates.get(Case1.keyWO).map(_.value)
      assert(result == Some(Case1.expectedAggregateValueWO))
    }
    
    "correctly processes tags with differend field values" in {
      val result = storage.aggregates.get(Case1.keyAll).map(_.value)
      assert(result == Some(Case1.expectedAggregateValueAll))
    }  
  }
  
}
