package io.github.JankaGramofonomanka.analyticsplatform.test.ops

import cats.effect._
import cats.effect.unsafe.implicits.global

import org.scalatest.freespec.AnyFreeSpec

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.test.TestUtils
import io.github.JankaGramofonomanka.analyticsplatform.test.ExampleData._

class OpsIntegrationSpec extends AnyFreeSpec {
  
  "case 1" - {
    val storage = TestUtils.Storage.empty
    val interface = TestUtils.getMocks(storage)

    val (frontend, proc) = TestUtils.getOps[IO](interface)

    val computation: IO[(Profile, Aggregates, Aggregates, Aggregates)] = for {
      _ <- frontend.storeTag(Case1.tagRS)
      _ <- frontend.storeTag(Case1.tagWO1)
      _ <- frontend.storeTag(Case1.tagWO2)

      _ <- proc.processTags.take(3*9).compile.drain

      aggregatesRS  <- frontend.getAggregates(Case1.timeRange, Case1.fieldsRS)
      aggregatesWO  <- frontend.getAggregates(Case1.timeRange, Case1.fieldsWO)
      aggregatesAll <- frontend.getAggregates(Case1.timeRange, Case1.fieldsAll)
      profile       <- frontend.getProfile(Case1.cookie, Case1.timeRange, 3)
      
    } yield (profile, aggregatesRS, aggregatesWO, aggregatesAll)

    val (profile, aggregatesRS, aggregatesWO, aggregatesAll) = computation.unsafeRunSync()

    "stored tags can be retrieved" in {
      
      assert(TestUtils.getTags(Case1.action, profile).contains(Case1.tagRS))
      assert(TestUtils.getTags(Case1.action, profile).contains(Case1.tagWO1))
      assert(TestUtils.getTags(Case1.action, profile).contains(Case1.tagWO2))
    }

    "tags are aggregated" - {
      
      "one tag" in {
        val expected = AggregateItem(Case1.bucket, Case1.expectedAggregateValueRS)
        assert(aggregatesRS.items.contains(expected))
      }
      
      "two tags" in {
        val expected = AggregateItem(Case1.bucket, Case1.expectedAggregateValueWO)
        assert(aggregatesWO.items.contains(expected))
      }
      
      "tags with differend field values" in {
        val expected = AggregateItem(Case1.bucket, Case1.expectedAggregateValueAll)
        assert(aggregatesAll.items.contains(expected))
      }
      
    }
  }

  "case 2" - {
    val storage = TestUtils.Storage.empty
    val interface = TestUtils.getMocks(storage)

    val (frontend, proc) = TestUtils.getOps[IO](interface)

    val computation: IO[(Profile, Profile)] = for {
      _ <- frontend.storeTag(Case2.tag1)
      _ <- frontend.storeTag(Case2.tag2)
      _ <- proc.processTags.take(2*9).compile.drain

      profile1 <- frontend.getProfile(Case2.cookie1, Case2.timeRange, 2)
      profile2 <- frontend.getProfile(Case2.cookie2, Case2.timeRange, 2)
      
    } yield (profile1, profile2)

    val (profile1, profile2) = computation.unsafeRunSync()

    "tags are stored in the right profiles" in {
      assert(TestUtils.getTags(Case2.action, profile1).contains(Case2.tag1))
      assert(TestUtils.getTags(Case2.action, profile2).contains(Case2.tag2))
    }
    
    "tags are not stored in the wrong profiles" in {
      assert(!TestUtils.getTags(Case2.action, profile1).contains(Case2.tag2))
      assert(!TestUtils.getTags(Case2.action, profile2).contains(Case2.tag1))
    }
    
  }

}

