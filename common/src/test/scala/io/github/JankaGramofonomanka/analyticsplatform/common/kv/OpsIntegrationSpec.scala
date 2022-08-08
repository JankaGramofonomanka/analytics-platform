package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import cats.effect._
import cats.effect.unsafe.implicits.global

import org.scalatest.freespec.AnyFreeSpec

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.TestUtils._
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.TestCaseData._

class OpsIntegrationSpec extends AnyFreeSpec {
  

  "case 1" - {
    val storage = Storage.empty
    val interface = getMocks[IO](storage)

    val (frontend, proc) = getOps[IO](interface)

    val computation: IO[(PrettyProfile, Aggregates, Aggregates, Aggregates)] = for {
      _ <- frontend.storeTag(Case1.tagRS)
      _ <- frontend.storeTag(Case1.tagWO1)
      _ <- frontend.storeTag(Case1.tagWO2)

      _ <- proc.processTags.take(3).compile.drain

      aggregatesRS  <- frontend.getAggregates(Case1.timeRange, Case1.fieldsRS)
      aggregatesWO  <- frontend.getAggregates(Case1.timeRange, Case1.fieldsWO)
      aggregatesAll <- frontend.getAggregates(Case1.timeRange, Case1.fieldsAll)
      profile       <- frontend.getProfile(Case1.cookie, Case1.timeRange, 3)
      
    } yield (profile, aggregatesRS, aggregatesWO, aggregatesAll)

    val (profile, aggregatesRS, aggregatesWO, aggregatesAll) = computation.unsafeRunSync()

    "stored tags can be retrieved" in {
      val simple = profile.simplify
      assert(simple.tags.contains(Case1.tagRS))
      assert(simple.tags.contains(Case1.tagWO1))
      assert(simple.tags.contains(Case1.tagWO2))
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
    val storage = Storage.empty
    val interface = getMocks[IO](storage)

    val (frontend, _) = getOps[IO](interface)

    val computation: IO[(SimpleProfile, SimpleProfile)] = for {
      _ <- frontend.storeTag(Case2.tag1)
      _ <- frontend.storeTag(Case2.tag2)

      profile1 <- frontend.getProfile(Case2.cookie1, Case2.timeRange, 2)
      profile2 <- frontend.getProfile(Case2.cookie2, Case2.timeRange, 2)
      
    } yield (profile1.simplify, profile2.simplify)

    val (profile1, profile2) = computation.unsafeRunSync()

    "tags are stored in the right profiles" in {
      assert(profile1.tags.contains(Case2.tag1))
      assert(profile2.tags.contains(Case2.tag2))
    }
    
    "tags are not stored in the wrong profiles" in {
      assert(!profile1.tags.contains(Case2.tag2))
      assert(!profile2.tags.contains(Case2.tag1))
    }
    
  }

}

