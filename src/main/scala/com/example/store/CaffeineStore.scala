package com.example.store

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalacache.serialization.{Codec, InMemoryRepr}
import scalacache.{Entry, LoggingSupport}

/**
  * Wrapper around Caffeine {@see https://github.com/blemale/scaffeine}.
  * Inspired and adapted from scalacache {@see https://github.com/cb372/scalacache }
  */
class CaffeineStore(underlying: CCache[String, Object])
  extends JobStore[InMemoryRepr]
    with LoggingSupport {

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Get the value corresponding to the given key from the cache
    *
    * @param key cache key
    * @tparam V the type of the corresponding value
    * @return the value, if there is one
    */
  override def get[V](key: String)(implicit codec: Codec[V, InMemoryRepr]) = {
    /*
    Note: we could delete the entry from the cache if it has expired,
    but that would lead to nasty race conditions in case of concurrent access.
    We might end up deleting an entry that another thread has just inserted.
    */
    val baseValue = underlying.getIfPresent(key)
    val result = {
      if (baseValue != null) {
        val entry = baseValue.asInstanceOf[Entry[V]]
        if (entry.isExpired) None else Some(entry.value)
      } else None
    }
    if (logger.isDebugEnabled)
      logCacheHitOrMiss(key, result)
    Future.successful(result)
  }

  /**
    * Insert the given key-value pair into the cache, with an optional Time To Live.
    *
    * @param key   cache key
    * @param value corresponding value
    * @param ttl   Time To Live
    * @tparam V the type of the corresponding value
    */
  override def put[V](key: String, value: V, ttl: Option[Duration] = None)(implicit codec: Codec[V, InMemoryRepr]) = {
    val entry = Entry(value, ttl.map(toExpiryTime))
    underlying.put(key, entry.asInstanceOf[Object])
    logCachePut(key, ttl)
    Future.successful(())
  }

  /**
    * Remove the given key and its associated value from the cache, if it exists.
    * If the key is not in the cache, do nothing.
    *
    * @param key cache key
    */
  override def remove(key: String) = Future.successful(underlying.invalidate(key))

  override def removeAll() = Future.successful(underlying.invalidateAll())

  override def close(): Unit = {
    // Nothing to do
  }

  /**
    * Returns a ConcurrentMap of the underlying data in the cache. Any changes to the data in the
    * map returned will not be reflected in the cache.
    *
    * Cache values that have expired will not be returned in the map.
    * @tparam String Key type
    * @tparam V Value type
    * @return a ConcurrentMap of the data in the cache.
    */
  override def asMap[String, V](): Future[ConcurrentMap[String, V]] = {
    val cacheMap: ConcurrentMap[String, V] =
      new ConcurrentHashMap[String, V](underlying.asMap().asInstanceOf[ConcurrentMap[String, V]])

    this.synchronized {
      for ((k, v) <- cacheMap) {
        expireEntryIfPastTtl(v.asInstanceOf[Entry[V]]) match {
          case None => cacheMap.remove(k)
          case Some(_) => cacheMap.put(k, v.asInstanceOf[Entry[V]].value)
        }
      }
    }

    Future.successful(cacheMap)
  }

  private def expireEntryIfPastTtl[V](entry: Entry[V]): Option[V] = {
    if (entry.isExpired) None else Some(entry.value)
  }

  private def toExpiryTime(ttl: Duration): DateTime = DateTime.now.plus(ttl.toMillis)

}

object CaffeineStore {

  /**
    * Create a new Caffeine cache
    */
  def apply(): CaffeineStore = apply(Caffeine.newBuilder().build[String, Object]())

  /**
    * Create a new cache utilizing the given underlying Caffeine cache.
    *
    * @param underlying a Caffeine cache
    */
  def apply(underlying: CCache[String, Object]): CaffeineStore = new CaffeineStore(underlying)
}