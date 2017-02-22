package com.example.store

import java.util.concurrent.ConcurrentMap

import scala.concurrent.Future
import scalacache.Cache
import scalacache.serialization.{Codec, InMemoryRepr}

/**
  * Adapted from scalacache {@see https://github.com/cb372/scalacache } and modified to implement the asMap() method
  * that will return a view of the entries in the stored as a thread-safe map.
  */
trait JobStore[Repr] extends Cache[Repr]{
  def asMap[String, V](): Future[ConcurrentMap[String, V]]
}
