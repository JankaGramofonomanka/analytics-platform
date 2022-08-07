package io.github.JankaGramofonomanka.analyticsplatform.common

import org.scalatest.freespec.AnyFreeSpec

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.ExampleData
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils

class BasicDataTransformationsSpec extends AnyFreeSpec {
  "`Timestamp.getBucket`" - {
    "buckets are rounded up to 1 minute" in {
      val bucket = ExampleData.timestamp.getBucket
      assert(bucket.toDateTime.getSecond == 0)
      assert(bucket.toDateTime.getNano == 0)
    }
    "bucket is not after timestamp" in {
      val bucket = ExampleData.timestamp.getBucket
      assert(!bucket.toTimestamp.isAfter(ExampleData.timestamp))
    }
    "in composition with `Bucket.toTimestamp`" in {
      val timestamp = ExampleData.bucket.toTimestamp
      assert(timestamp.getBucket == ExampleData.bucket)
      assert(timestamp.getBucket.toTimestamp == timestamp)
    }
  }
  "Bucket.addMinutes" in {
    val bucket  = ExampleData.bucket
    val before  = bucket.addMinutes(-1)
    val now     = bucket.addMinutes(0)
    val after   = bucket.addMinutes(1)

    assert(now    .isEqual(bucket))

    assert(before .isBefore(now))
    assert(now    .isBefore(after))
    assert(before .isBefore(after))
    
  }
  "`TimeRange.contains`" in {
    val from      = ExampleData.timestamp.getBucket.toTimestamp
    val to        = ExampleData.timestamp.getBucket.addMinutes(1).toTimestamp
    val timeRange = TimeRange(from, to)
    assert(timeRange.contains(ExampleData.timestamp))
    assert(timeRange.contains(from))
    assert(!timeRange.contains(to))

    val before  = from.getBucket.addMinutes(-1) .toTimestamp
    val after   = to  .getBucket.addMinutes(1)  .toTimestamp
    assert(!timeRange.contains(before))
    assert(!timeRange.contains(after))
  }
  "`TimeRange.getBuckets`" - {
    "1 minute range" in {
      val from  = ExampleData.bucket
      val to    = ExampleData.bucket.addMinutes(1)

      val timeRange = TimeRange(from.toTimestamp, to.toTimestamp)
      val buckets = timeRange.getBuckets
      assert(buckets.length == 1)
      assert(buckets.contains(from))
      assert(!buckets.contains(to))
    }
    "2 minute range" in {
      val from    = ExampleData.bucket
      val between = ExampleData.bucket.addMinutes(1)
      val to      = ExampleData.bucket.addMinutes(2)

      val timeRange = TimeRange(from.toTimestamp, to.toTimestamp)
      val buckets = timeRange.getBuckets
      assert(buckets.length == 2)
      assert(buckets.contains(from))
      assert(buckets.contains(between))
      assert(!buckets.contains(to))
    }
  }
  "`SimpleProfile.update`" - {

    val tagsToKeep = 5
    val range = Range.Long(0, tagsToKeep.toLong, 1)
    val timestamps = range.map(n => ExampleData.bucket.addMinutes(-n).toTimestamp)
    val tags = timestamps.map(ts => ExampleData.userTag.copy(time = ts)).toVector
    
    val profile = SimpleProfile(tags)
    val updated = profile.update(ExampleData.userTag, tagsToKeep)

    "added tag is remains in the profile" in assert(updated.tags.contains(ExampleData.userTag))
    "limited number of tags is kept"      in assert(updated.tags.length <= Config.Other.numTagsToKeep)
    "tags are sorted after update"        in {
      val sorted: Seq[UserTag] => Boolean = Utils.isSortedWith((tag1, tag2) => !tag1.time.isBefore(tag2.time))
      assert(sorted(updated.tags))

      val past = ExampleData.bucket.addMinutes(-3).toTimestamp
      val notFresh = ExampleData.userTag.copy(time = past)
      assert(sorted(profile.update(notFresh, tagsToKeep).tags))
    }
  }
  "PrettyProfile.simplify, SimpleProfile.prettify" - {
    "simplify . prettify" in {
      val simple = ExampleData.simpleProfile
      val cookie = ExampleData.cookie
      assert(simple.prettify(cookie).simplify == simple)
    }

    "prettify . simplify" in {
      val pretty = ExampleData.prettyProfile
      assert(pretty.simplify.prettify(pretty.cookie) == pretty)
    }
    
  }
}

