package com.example

import spray.json._

case class Job(id: String, total: Double, progress: Double)

// Used to deserialize the body of the PUT request to just the fields
// of Job that we are allowing to be changed
case class JobUpdate(progress: Double)

case class JobIncrement(increment: Double)

object JobJsonProtocol extends DefaultJsonProtocol {
  implicit val jobFormat = jsonFormat3(Job)
  implicit val jobUpdateFormat = jsonFormat1(JobUpdate.apply)
  implicit val jobIncrementFormat = jsonFormat1(JobIncrement.apply)
}

