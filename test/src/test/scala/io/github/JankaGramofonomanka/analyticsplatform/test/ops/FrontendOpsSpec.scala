package io.github.JankaGramofonomanka.analyticsplatform.test.ops

import org.scalatest.freespec.AnyFreeSpec

import cats.effect._
import cats.effect.unsafe.implicits.global

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.test.ExampleData.General
import io.github.JankaGramofonomanka.analyticsplatform.test.TestUtils



class FrontendOpsSpec extends AnyFreeSpec {

  "`FrontendOps.storeTag`" - {

    {
      val storage = TestUtils.Storage.empty
      val interface = TestUtils.getMocks(storage)

      val (frontend, _) = TestUtils.getOps[IO](interface)

      val tag = General.userTag
      frontend.storeTag(tag).unsafeRunSync()

      // TODO move this somewhere (no longer true since frontend no longer stores tags)?
      /*
      "stores the tag" in {
        val profile = storage.profiles.get(tag.cookie)
        assert(profile.nonEmpty)
        assert(profile.get.value.tags.contains(tag))
      }
      */

      "publishes the tag" in {
        val expected = tag
        val actual = storage.queue.dequeue()
        assert(expected == actual)
      }
    }
  }

  "`FrontendOps.getProfile`" - {

    val storage = TestUtils.Storage.empty
    val interface = TestUtils.getMocks(storage)

    val (frontend, _) = TestUtils.getOps[IO](interface)

    "returns empty profile when the profile is not stored" in {
      val returned = frontend
                      .getProfile(General.cookie, General.timeRange, General.limit)
                      .unsafeRunSync()
      assert(returned.views .length == 0)
      assert(returned.buys  .length == 0)
    }

    "returns the stored profile" in {
    
      val tag = General.userTag
      val profile = SimpleProfile(Vector(tag))
      storage.profiles.put(tag.cookie, TrackGen.default(profile))

      val timeRange = TestUtils.getTimeRangeContaining(tag.time)
      val limit     = 20
      
      val expected  = profile.prettify(tag.cookie)
      val actual    = frontend.getProfile(tag.cookie, timeRange, limit).unsafeRunSync()
      
      assert(expected == actual)
    }

    "takes time range into account" in {
      
      val plus0 = General.bucket
      val plus1 = General.bucket.addMinutes(1)
      val plus2 = General.bucket.addMinutes(2)
      val plus3 = General.bucket.addMinutes(3)
      
      val cookie = General.userTag.cookie
      val included = General.userTag.copy(time = plus1.toTimestamp)
      val excluded = General.userTag.copy(time = plus3.toTimestamp)

      val timeRange = TimeRange(plus0.toDateTime, plus2.toDateTime)

      val profile = SimpleProfile(Vector(included, excluded))
      storage.profiles.put(cookie, TrackGen.default(profile))

      val limit = 20
      
      val returned = frontend.getProfile(cookie, timeRange, limit).unsafeRunSync()
      
      val simple = returned.simplify
      
      assert(simple.tags.contains(included))
      assert(!simple.tags.contains(excluded))
    }

    "takes limit into account" in {
      val plus0 = General.bucket
      val plus1 = General.bucket.addMinutes(1)
      val plus2 = General.bucket.addMinutes(2)
      val plus3 = General.bucket.addMinutes(3)
      
      val cookie = General.userTag.cookie
      val included = General.userTag.copy(time = plus2.toTimestamp)
      val excluded = General.userTag.copy(time = plus1.toTimestamp)

      val timeRange = TimeRange(plus0.toDateTime, plus3.toDateTime)

      val profile = SimpleProfile(Vector(included, excluded))
      storage.profiles.put(cookie, TrackGen.default(profile))

      val returned = frontend.getProfile(cookie, timeRange, 1).unsafeRunSync()
      
      val simple = returned.simplify
      
      assert(simple.tags.contains(included))
      assert(!simple.tags.contains(excluded))
    }
  }

  "`FrontendOps.getAggregates`" - {
    val storage = TestUtils.Storage.empty
    val interface = TestUtils.getMocks(storage)

    val (frontend, _) = TestUtils.getOps[IO](interface)

    val fields  = General.aggregateFields
    val key     = AggregateKey.fromFields(General.bucket, fields)
    val value   = General.aggregateValue

    storage.aggregates.put(key, TrackGen.default(value))

    val from      = key.bucket.addMinutes(-1)
    val to        = key.bucket.addMinutes(2)
    val timeRange = TimeRange(from.toDateTime, to.toDateTime)
    
    val returned = frontend.getAggregates(timeRange, fields).unsafeRunSync()
    
    val buckets = returned.items.map(_.bucket)

    "returns stored aggregates" in assert(returned.items.contains(AggregateItem(key.bucket, value)))
    "returns agggregates with correct fields" in assert(returned.fields == fields)
    "returns sorted aggregates" in {
      assert(Utils.isSortedWith((b1: Bucket, b2: Bucket) => !b1.isAfter(b2))(buckets))
    }
    "returns aggregates in the given time range" in {
      assert(buckets.head == from)
      assert(buckets.last == to.addMinutes(-1))
    }
  }
}
