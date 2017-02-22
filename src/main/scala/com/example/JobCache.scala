package com.example

import com.example.store._

//import scalacache._
//import guava._

object JobCache {
  implicit val jobCache = ScalaCache(CaffeineStore()).cache
}
