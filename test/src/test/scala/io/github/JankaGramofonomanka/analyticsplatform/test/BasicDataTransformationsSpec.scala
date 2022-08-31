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

  {
    val tagsSorted: Seq[UserTag] => Boolean
      = Utils.isSortedWith((tag1, tag2) => !tag1.time.isBefore(tag2.time))
    val cookie = General.userTag.cookie

    // Just in case
    "[validation of test data]" in {
      assert(cookie == General.userTagV.cookie)
      assert(cookie == General.userTagB.cookie)
    }
    
    "`Profile.addOne`" - {

      val range = Range.Long(0, 5, 1)
      val timestamps = range.map(n => General.bucket.addMinutes(-n).toTimestamp)
      val tagsV = timestamps.map(ts => General.userTagV.copy(time = ts)).toVector
      val tagsB = timestamps.map(ts => General.userTagB.copy(time = ts)).toVector
      
      val profile = TestUtils.fromTags(cookie, tagsV ++ tagsB)
      val updated = profile.addOne(General.userTagV).addOne(General.userTagB)

      "added tag remains in the profile"  in {
        assert(TestUtils.profileContains(updated, General.userTagV))
        assert(TestUtils.profileContains(updated, General.userTagB))
      }
      "added tag is in the right tag list" in {
        assert(updated.views.contains(General.userTagV))
        assert(updated.buys.contains(General.userTagB))
      }
      "added tag is not in the wrong list" in {
        assert(!updated.views.contains(General.userTagB))
        assert(!updated.buys.contains(General.userTagV))
      }
      "tags are sorted after update"      in {
        assert(tagsSorted(updated.views))
        assert(tagsSorted(updated.buys))

        val past = General.bucket.addMinutes(-3).toTimestamp
        val notFreshV = General.userTagV.copy(time = past)
        val notFreshB = General.userTagB.copy(time = past)
        val updated2 = profile.addOne(notFreshV).addOne(notFreshB)
        assert(tagsSorted(updated2.views))
        assert(tagsSorted(updated2.buys))
      }
    }

    "`Profile.limit`" - {

      val tagsToKeep = 5
      val range = Range.Long(0, 10, 1)
      val timestamps = range.map(n => General.bucket.addMinutes(-n).toTimestamp)
      val tagsV = timestamps.map(ts => General.userTagV.copy(time = ts)).toVector
      val tagsB = timestamps.map(ts => General.userTagB.copy(time = ts)).toVector
      
      val profile = TestUtils.fromTags(cookie, tagsV ++ tagsB)
      val updated = profile.limit(tagsToKeep)

      "limited number of tags is kept" in {
        assert(updated.views.length <= tagsToKeep)
        assert(updated.buys .length <= tagsToKeep)
      }
    }

    "`Profile.++`" - {

      val range = Range.Long(0, 5, 1)
      val timestamps = range.map(n => General.bucket.addMinutes(-n).toTimestamp)
      val tagsV = timestamps.map(ts => General.userTagV.copy(time = ts)).toVector
      val tagsB = timestamps.map(ts => General.userTagB.copy(time = ts)).toVector
      
      val profileV  = TestUtils.fromTags(cookie, tagsV)
      val profileB  = TestUtils.fromTags(cookie, tagsB)
      val profileVB = TestUtils.fromTags(cookie, tagsV ++ tagsB)

      "sums lengths" in {
        
        for (profile1 <- List(profileV, profileB, profileVB)) {
          for (profile2 <- List(profileV, profileB, profileVB)) {
            val sum = profile1 ++ profile2
          
            assert(sum.views.length == profile1.views.length + profile2.views.length)
            assert(sum.buys.length == profile1.buys.length + profile2.buys.length)
          }
        }

        
      }
      "tags are sorted after concatenation" in {

        for (profile1 <- List(profileV, profileB, profileVB)) {
          for (profile2 <- List(profileV, profileB, profileVB)) {
            val sum = profile1 ++ profile2
          
            assert(tagsSorted(sum.views))
            assert(tagsSorted(sum.buys))
          }
        }
      }
    }
  }
  
  "`AggregateValue.update`" - {
    val updated = General.aggregateValue.update(General.price)
    "increases count by 1" in assert(updated.count == General.aggregateValue.count + 1)
    "increases price" in assert(updated.sumPrice == General.aggregateValue.sumPrice + General.price)
  }
  "`AggregateValue.+`" - {
    val value1 = AggregateValue(5, Price(100))
    val value2 = AggregateValue(10, Price(50))
    val sum = value1 + value2
    
    "sums counts" in assert(sum.count == value1.count + value2.count)
    "sums prices" in assert(sum.sumPrice == value1.sumPrice + value2.sumPrice)
  }
  "`AggregateValue.fromTag`" - {
    val value = AggregateValue.fromTag(General.userTag)
    "sets count to 1" in assert(value.count == 1)
    "sets price to product price" in assert(value.sumPrice == General.userTag.productInfo.price)
  }

  "`AggregateVB.update`" - {
    val updatedV = General.aggregateVB.update(VIEW, General.price)
    val updatedB = General.aggregateVB.update(BUY, General.price)
    "increases count by 1 in respective aggregate value" in {
      assert(updatedV.views.count == General.aggregateVB.views.count + 1)
      assert(updatedB.buys.count == General.aggregateVB.buys.count + 1)
    }
    "does not increase count in the other aggregate value" in {
      assert(updatedV.buys.count == General.aggregateVB.buys.count)
      assert(updatedB.views.count == General.aggregateVB.views.count)
    }
    "increases price in respective aggregate value" in {
      assert(updatedV.views.sumPrice == General.aggregateVB.views.sumPrice + General.price)
      assert(updatedB.buys.sumPrice == General.aggregateVB.buys.sumPrice + General.price)
    }
    "does not increase price in the other aggregate value" in {
      assert(updatedV.buys.sumPrice == General.aggregateVB.buys.sumPrice)
      assert(updatedB.views.sumPrice == General.aggregateVB.views.sumPrice)
    }
  }
  "`AggregateVB.+`" - {
    val value1 = AggregateValue(5, Price(100))
    val value2 = AggregateValue(10, Price(50))
    val vb12 = AggregateVB(value1, value2)
    val vb21 = AggregateVB(value2, value1)
    val sum1221 = vb12 + vb21
    val sum1212 = vb12 + vb12
    
    "sums counts" in {
      assert(sum1221.views.count == value1.count + value2.count)
      assert(sum1221.buys .count == value1.count + value2.count)
      assert(sum1212.views.count == value1.count + value1.count)
      assert(sum1212.buys .count == value2.count + value2.count)
    }
    "sums prices" in {
      assert(sum1221.views.sumPrice == value1.sumPrice + value2.sumPrice)
      assert(sum1221.buys .sumPrice == value1.sumPrice + value2.sumPrice)
      assert(sum1212.views.sumPrice == value1.sumPrice + value1.sumPrice)
      assert(sum1212.buys .sumPrice == value2.sumPrice + value2.sumPrice)
    }
  }
  "`AggregateVB.fromTag`" - {
    val valueV = AggregateVB.fromTag(General.userTagV)
    val valueB = AggregateVB.fromTag(General.userTagB)
    "sets count to 1 in the respective aggregate value" in {
      assert(valueV.views .count == 1)
      assert(valueB.buys  .count == 1)
    }
    "leaves count at 0 in the ther aggregate value" in {
      assert(valueV.buys  .count == 0)
      assert(valueB.views .count == 0)
    }
    "sets price to product price" in {
      assert(valueV.views .sumPrice == General.userTagV.productInfo.price)
      assert(valueB.buys  .sumPrice == General.userTagB.productInfo.price)
    }
    "leaves price at 0 in the other aggregate value" in {
      assert(valueV.buys  .sumPrice == Price(0))
      assert(valueB.views .sumPrice == Price(0))
    }
  }
}

