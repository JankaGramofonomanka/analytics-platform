package io.github.JankaGramofonomanka.analyticsplatform.common.kv

import org.scalatest.freespec.AnyFreeSpec

import cats.effect._
import cats.effect.unsafe.implicits.global

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.Config
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.common.kv.OpsSpecUtils._



class FrontendOpsSpec extends AnyFreeSpec {

  "`FrontendOps.storeTag`" - {

    val storage = Storage.empty
    val interface = getMocks[IO](storage)

    val (frontend, _) = getOps[IO](interface)

    val tag = ExampleData.userTag
    frontend.storeTag(tag).unsafeRunSync()

    "stores the tag" in {
      val profile = storage.profiles.get(tag.cookie)
      assert(profile.nonEmpty)
      assert(profile.get.tags.contains(tag))
    }

    "publishes the tag" in {
      val expected = tag
      val actual = storage.queue.dequeue()
      assert(expected == actual)
    }
  }

  "`FrontendOps.getProfile`" - {

    val storage = Storage.empty
    val interface = getMocks[IO](storage)

    val (frontend, _) = getOps[IO](interface)

    "returns empty profile when the profile is not stored" in {
      val returned = frontend
                      .getProfile(ExampleData.cookie, ExampleData.timeRange, ExampleData.limit)
                      .unsafeRunSync()
      assert(returned.views .length == 0)
      assert(returned.buys  .length == 0)
    }

    "returns the stored profile" in {
    
      val tag = ExampleData.userTag
      val profile = SimpleProfile(Array(tag))
      storage.profiles.put(tag.cookie, profile)

      val timeRange = getTimeRangeContaining(tag.time)
      val limit     = Config.Other.numTagsToKeep
      
      val expected  = profile.prettify(tag.cookie)
      val actual    = frontend.getProfile(tag.cookie, timeRange, limit).unsafeRunSync()
      
      assert(expected == actual)
    }

    "takes time range into account" in {
      
      val plus0 = ExampleData.bucket.toTimestamp
      val plus1 = ExampleData.bucket.addMinutes(1).toTimestamp
      val plus2 = ExampleData.bucket.addMinutes(2).toTimestamp
      val plus3 = ExampleData.bucket.addMinutes(3).toTimestamp
      
      val cookie = ExampleData.userTag.cookie
      val included = ExampleData.userTag.copy(time = plus1)
      val excluded = ExampleData.userTag.copy(time = plus3)

      val timeRange = TimeRange(plus0, plus2)

      val profile = SimpleProfile(Array(included, excluded))
      storage.profiles.put(cookie, profile)

      val limit     = Config.Other.numTagsToKeep
      
      val returned = frontend.getProfile(cookie, timeRange, limit).unsafeRunSync()
      
      val simple = returned.simplify
      
      assert(simple.tags.contains(included))
      assert(!simple.tags.contains(excluded))
    }

    "takes limit into account" in {
      val plus0 = ExampleData.bucket.toTimestamp
      val plus1 = ExampleData.bucket.addMinutes(1).toTimestamp
      val plus2 = ExampleData.bucket.addMinutes(2).toTimestamp
      val plus3 = ExampleData.bucket.addMinutes(3).toTimestamp
      
      val cookie = ExampleData.userTag.cookie
      val included = ExampleData.userTag.copy(time = plus2)
      val excluded = ExampleData.userTag.copy(time = plus1)

      val timeRange = TimeRange(plus0, plus3)

      val profile = SimpleProfile(Array(included, excluded))
      storage.profiles.put(cookie, profile)

      val returned = frontend.getProfile(cookie, timeRange, 1).unsafeRunSync()
      
      val simple = returned.simplify
      
      assert(simple.tags.contains(included))
      assert(!simple.tags.contains(excluded))
    }
  }

  "`FrontendOps.getAggregates`" - {
    val storage = Storage.empty
    val interface = getMocks[IO](storage)

    val (frontend, _) = getOps[IO](interface)

    val fields  = ExampleData.aggregateFields
    val key     = AggregateKey.fromFields(ExampleData.bucket, fields)
    val value   = ExampleData.aggregateValue

    storage.aggregates.put(key, value)

    val from      = key.bucket.addMinutes(-1).toTimestamp
    val to        = key.bucket.addMinutes(2) .toTimestamp
    val timeRange = TimeRange(from, to)
    
    val returned = frontend.getAggregates(timeRange, fields).unsafeRunSync()
    
    val buckets = returned.items.map(_.bucket)

    "returns stored aggregates" in assert(returned.items.contains(AggregateItem(key.bucket, value)))
    "returns agggregates with correct fields" in assert(returned.fields == fields)
    "returns sorted aggregates" in {
      assert(Utils.isSortedWith((b1: Bucket, b2: Bucket) => !b1.value.isAfter(b2.value))(buckets))
    }
    "returns aggregates in the given time range" in {
      assert(buckets.head == from.getBucket)
      assert(buckets.last == to.getBucket.addMinutes(-1))
    }
  }
}
