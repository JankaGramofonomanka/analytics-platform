package io.github.JankaGramofonomanka.analyticsplatform.test

import org.scalatest.freespec.AnyFreeSpec

import io.github.JankaGramofonomanka.analyticsplatform.common.Data._
import io.github.JankaGramofonomanka.analyticsplatform.common.Utils
import io.github.JankaGramofonomanka.analyticsplatform.test.ExampleData._

class BasicDataTransformationsSpec extends AnyFreeSpec {
  "`Timestamp.getBucket`" - {
    "buckets are rounded up to 1 minute" in {
      val bucket = General.timestamp.getBucket
      assert(bucket.toDateTime.getSecond == 0)
      assert(bucket.toDateTime.getNano == 0)
    }
    "bucket is not after timestamp" in {
      val bucket = General.timestamp.getBucket
      assert(!bucket.toTimestamp.isAfter(General.timestamp))
    }
    "in composition with `Bucket.toTimestamp`" in {
      val timestamp = General.bucket.toTimestamp
      assert(timestamp.getBucket == General.bucket)
      assert(timestamp.getBucket.toTimestamp == timestamp)
    }
  }
  "Bucket.addMinutes" in {
    val bucket  = General.bucket
    val before  = bucket.addMinutes(-1)
    val now     = bucket.addMinutes(0)
    val after   = bucket.addMinutes(1)

    assert(now    .isEqual(bucket))

    assert(before .isBefore(now))
    assert(now    .isBefore(after))
    assert(before .isBefore(after))
    
  }
  "`TimeRange.contains`" in {
    val from      = General.timestamp.getBucket
    val to        = General.timestamp.getBucket.addMinutes(1)
    val timeRange = TimeRange(from.toDateTime, to.toDateTime)
    assert(timeRange.contains(General.timestamp))
    assert(timeRange.contains(from.toTimestamp))
    assert(!timeRange.contains(to.toTimestamp))

    val before  = from.addMinutes(-1) .toTimestamp
    val after   = to  .addMinutes(1)  .toTimestamp
    assert(!timeRange.contains(before))
    assert(!timeRange.contains(after))
  }
  "`TimeRange.getBuckets`" - {
    "1 minute range" in {
      val from  = General.bucket
      val to    = General.bucket.addMinutes(1)

      val timeRange = TimeRange(from.toDateTime, to.toDateTime)
      val buckets = timeRange.getBuckets
      assert(buckets.length == 1)
      assert(buckets.contains(from))
      assert(!buckets.contains(to))
    }
    "2 minute range" in {
      val from    = General.bucket
      val between = General.bucket.addMinutes(1)
      val to      = General.bucket.addMinutes(2)

      val timeRange = TimeRange(from.toDateTime, to.toDateTime)
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
    val timestamps = range.map(n => General.bucket.addMinutes(-n).toTimestamp)
    val tags = timestamps.map(ts => General.userTag.copy(time = ts)).toVector
    
    val profile = SimpleProfile(tags)
    val updated = profile.update(General.userTag, tagsToKeep)

    "added tag remains in the profile"  in assert(updated.tags.contains(General.userTag))
    "limited number of tags is kept"    in assert(updated.tags.length <= tagsToKeep)
    "tags are sorted after update"      in {
      val sorted: Seq[UserTag] => Boolean = Utils.isSortedWith((tag1, tag2) => !tag1.time.isBefore(tag2.time))
      assert(sorted(updated.tags))

      val past = General.bucket.addMinutes(-3).toTimestamp
      val notFresh = General.userTag.copy(time = past)
      assert(sorted(profile.update(notFresh, tagsToKeep).tags))
    }
  }
  "PrettyProfile.simplify, SimpleProfile.prettify" - {
    "simplify . prettify" in {
      val simple = General.simpleProfile
      val cookie = General.cookie
      assert(simple.prettify(cookie).simplify == simple)
    }

    "prettify . simplify" in {
      val pretty = General.prettyProfile
      assert(pretty.simplify.prettify(pretty.cookie) == pretty)
    }
    
  }
}
