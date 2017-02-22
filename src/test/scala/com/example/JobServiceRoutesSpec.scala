package com.example

import com.example.JobCache._
import com.example.JobJsonProtocol._
import com.example._
import org.scalatest.{FreeSpec, Matchers}
import org.specs2.mutable.Before
import org.specs2.time.NoTimeConversions
import spray.http.{HttpEntity, MediaTypes}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest
import spray.httpx.SprayJsonSupport

import scala.concurrent.duration._

class JobServiceRoutesSpec extends FreeSpec with Matchers
  with ScalatestRouteTest with NoTimeConversions with HttpService
  with JobService with Before {

  def actorRefFactory = system

  val jobsLink = "/jobs"

  //  implicit def actorRefFactory = system.dispatcher.prepare()

  def before = {
    val oneMinute = 1.minutes
    jobCache.put("1", Job("1", 100, 1), ttl = Option(oneMinute))
    jobCache.put("2", Job("2", 100, 10), ttl = Option(oneMinute))
    jobCache.put("3", Job("3", 100, 20), ttl = Option(oneMinute))
    jobCache.put("4", Job("4", 100, 10), ttl = Option(oneMinute))
    jobCache.put("99", Job("99", 100, 1), ttl = Option(oneMinute))
  }

  "JobService" - {

    "return a job for GET request with identifier" in {
      Get(jobsLink + "/1") ~> jobRoute ~> check {
        contentType.toString should include("application/json")
        responseAs[String] === "{\n  \"id\": \"1\",\n  \"total\": 100.0,\n  \"progress\": 1.0\n}"
      }
    }

    "return all jobs for GET requests to the jobs path" in {
      Get(jobsLink) ~> jobRoute ~> check {
        status shouldEqual OK
        val response = responseAs[Seq[Job]]
        response.length shouldEqual 5
        response(0).total shouldEqual 100.0
      }
    }

    "update the progress of a job less than total will set progress value" in {
      Put(jobsLink + "/99",
        HttpEntity(MediaTypes.`application/json`,
          """{"progress":50}""")
      ) ~> jobRoute ~> check {
        status shouldEqual OK
        val response = responseAs[Job]
        response.progress shouldEqual 50.0
      }
    }

    "update the progress of a job more than total will set progress to total" in {
      Put(jobsLink + "/99",
        HttpEntity(MediaTypes.`application/json`,
          """{"progress":101}""")
      ) ~> jobRoute ~> check {
        status shouldEqual OK
        val response = responseAs[Job]
        response.progress shouldEqual 100.0
      }
    }

    "a negative progress value results in a server error" in {
      Put(jobsLink + "/99",
        HttpEntity(MediaTypes.`application/json`,
          """{"progress":-20}""")
      ) ~> jobRoute ~> check {
        //Expect IllegalArgumentException to be thrown
        status shouldEqual spray.http.StatusCodes.InternalServerError
      }
    }

    "increment the progress of a job should change progress value" in {
      Put(jobsLink + "/2",
        HttpEntity(MediaTypes.`application/json`,
          """{"increment":1}""")
      ) ~> jobRoute ~> check {
        status shouldEqual OK
        val response = responseAs[Job]
        response.progress shouldEqual 11.0
      }
    }

    "increment the progress of a job past the total should result in progress equal to total" in {
      Put(jobsLink + "/2",
        HttpEntity(MediaTypes.`application/json`,
          """{"increment":99}""")
      ) ~> jobRoute ~> check {
        status shouldEqual OK
        val response = responseAs[Job]
        response.progress shouldEqual 100.0
      }
    }

    "a negative value for increment results in server error" in {
      Put(jobsLink + "/2",
        HttpEntity(MediaTypes.`application/json`,
          """{"increment":-2}""")
      ) ~> jobRoute ~> check {
        //Expect IllegalArgumentException to be thrown
        status shouldEqual spray.http.StatusCodes.InternalServerError
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get() ~> jobRoute ~> check {
        handled shouldBe false
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> sealRoute(jobRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }
}
