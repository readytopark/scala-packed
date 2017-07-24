package io.findify.scalapacked

import java.util

import com.typesafe.scalalogging.LazyLogging
import io.findify.scalapacked.pool.{HeapPool, MemoryPool}

class StructMapImpl[A, B](var bucketCount: Int, var pool: MemoryPool = new HeapPool(1024))(implicit ke: Encoder[A], kd: Decoder[A], ve: Encoder[B], vd: Decoder[B]) extends LazyLogging {
  var buckets = new Array[Int](bucketCount)
  var usedBuckets = new util.BitSet(bucketCount)
  var count = 0

  private def findBucket(key: A): Int = {
    var i = key.hashCode() % bucketCount
    while (usedBuckets.get(i) && (key != kd.read(pool, buckets(i)))) {
      i = (i + 1) % bucketCount
    }
    i
  }

  def put(key: A, value: B): Unit = {
    if (count > (bucketCount / 2)) rebuild
    val offset = ke.write(key, pool)
    ve.write(value, pool)
    //logger.debug(s"put: k=$key, v=$value, offset=$offset, size=${pool.size}")

    val bucket = findBucket(key)
    if (usedBuckets.get(bucket)) {
      buckets(bucket) = offset
    } else {
      buckets(bucket) = offset
      usedBuckets.set(bucket)
      count += 1
    }
  }

  def get(key: A): Option[B] = {
    val bucket = findBucket(key)
    if (usedBuckets.get(bucket)) {
      val keySize = kd.size(pool, buckets(bucket))
      Some(vd.read(pool, buckets(bucket) + keySize))
    } else {
      None
    }
  }

  def rebuild = {
    val larger = new StructMapImpl[A,B](bucketCount * 2)
    //logger.debug(s"rebuild: size = ${larger.bucketCount}")
    var i = 0
    while (i < bucketCount) {
      if (usedBuckets.get(i)) {
        //logger.debug(s"reading key at ${buckets(i)}")
        val key = kd.read(pool, buckets(i))
        val keySize = kd.size(pool, buckets(i))
        val value = vd.read(pool, buckets(i) + keySize)
        larger.put(key, value)
      }
      i += 1
    }
    bucketCount = larger.bucketCount
    buckets = larger.buckets
    usedBuckets = larger.usedBuckets
    count = larger.count
    pool = larger.pool
    //logger.debug("rebuild done")
  }
}
