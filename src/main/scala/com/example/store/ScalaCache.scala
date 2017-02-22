package com.example.store

import scalacache.{CacheConfig, CacheKeyBuilder, DefaultCacheKeyBuilder}
import scalacache.memoization.MemoizationConfig

/**
  * Adapted from scalacache {@see https://github.com/cb372/scalacache } and modified to provide an asMap() method
  * that will return a view of the entries in the stored as a thread-safe map.
  *
  * Container holding the cache itself, along with all necessary configuration.
  */
case class ScalaCache[Repr] (
  cache: JobStore[Repr],
  cacheConfig: CacheConfig = CacheConfig(),
  keyBuilder: CacheKeyBuilder = DefaultCacheKeyBuilder,
  memoization: MemoizationConfig = MemoizationConfig())

