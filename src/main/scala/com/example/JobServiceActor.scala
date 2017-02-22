package com.example

import java.util.concurrent.ConcurrentMap

import akka.actor._
import com.example.JobCache._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.http._
import spray.routing._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class JobServiceActor extends Actor with JobService with ActorLogging {
  protected final val logger = LoggerFactory.getLogger(getClass.getName)

  override def cacheTtl: Duration = config.getLong("cache.ttl.minutes").minutes

  def actorRefFactory = context

  def receive = runRoute(jobRoute)
}

// this trait defines our service behavior independently from the service actor
trait JobService extends HttpService {

  import com.example.JobJsonProtocol._
  import spray.httpx.SprayJsonSupport.{sprayJsonUnmarshaller, _}

  val config = ConfigFactory.load()

  val jobRoute =
    pathPrefix("jobs") {
      post {
        respondWithMediaType(MediaTypes.`application/json`) {
          entity(as[Job]) { job =>
            // transfer to newly spawned actor
            detach(global) {
              complete(createJob(job))
            }
          }
        }
      } ~
        path(Segment) { jobId =>
          get {
            respondWithMediaType(MediaTypes.`application/json`) {
              onComplete(getJob(id = jobId)) {
                case Success(job) => complete(job)
                case Failure(e) => complete(StatusCodes.BadRequest)
              }
            }
          } ~
            put {
              respondWithMediaType(MediaTypes.`application/json`) {
                entity(as[JobUpdate]) { jobToUpdate =>
                  complete(updateJobProgress(id = jobId, jobToUpdate))
                }
              }
            } ~
            put {
              respondWithMediaType(MediaTypes.`application/json`) {
                entity(as[JobIncrement]) { jobIncrement =>
                  complete(incrementJobProgress(id = jobId, jobIncrement))
                }
              }
            } ~
            delete {
              complete {
                deleteJob(jobId)
                StatusCodes.NoContent
              }
            }
        } ~
        get {
          respondWithMediaType(MediaTypes.`application/json`) {
            onComplete(getAllJobs) {
              case Success(jobList) => complete(jobList)
              case Failure(e) => complete(StatusCodes.BadRequest)
            }
          }
        }
    }

  def cacheTtl: Duration = {
    1.minutes
  }

  def generateIdentifier: String = {
    java.util.UUID.randomUUID.toString
  }

  def createJob(job: Job): Future[Option[Job]] = {
    val id: String = generateIdentifier
    Future {
      Option(storeJob(Job(id, job.total, job.progress)))
    }
  }

  def getJob(id: String): Future[Option[Job]] = {
    jobCache.get(id)
  }

  def getAllJobs(): Future[Option[Array[Job]]] = {
    val jobMap: Future[ConcurrentMap[String, Job]] = jobCache.asMap()
    for {
      jobs <- jobMap
    } yield extractJobsFromMap(jobs)
  }

  def deleteJob(id: String): Future[Unit] = {
    jobCache.remove(id)
  }

  def storeJob(job: Job): Job = {
    jobCache.put(job.id, job, ttl = Option(cacheTtl))
    job
  }

  def extractJobsFromMap(jobMap: ConcurrentMap[String, Job]): Option[Array[Job]] = {
    Option(jobMap.values().asScala.toArray)
  }

  def updateJobProgress(id: String, jobUpdate: JobUpdate): Future[Option[Job]] = {
    if (jobUpdate.progress < 0) throw new IllegalArgumentException("Value of progress must be a positive value.")

    val currentJob: Future[Option[Job]] = getJob(id).map {
      case Some(Job(id, total, oldProgress)) =>
        if (jobUpdate.progress <= total) {
          Option(storeJob(Job(id, total, jobUpdate.progress)))
        } else {
          Option(storeJob(Job(id, total, total)))
        }
      case None => None
    }
    currentJob
  }

  def incrementJobProgress(id: String, jobIncrement: JobIncrement): Future[Option[Job]] = {
    if (jobIncrement.increment < 0) throw new IllegalArgumentException("Value of increment must be a positive value.")

    val currentJob: Future[Option[Job]] = getJob(id).map {
      case Some(Job(id, total, oldProgress)) =>
        if ((jobIncrement.increment + oldProgress) <= total) {
          Option(storeJob(Job(id, total, jobIncrement.increment + oldProgress)))
        } else {
          Option(storeJob(Job(id, total, total)))
        }
      case None => None
    }
    currentJob
  }

}

