package com.example

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

object Boot extends App {

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(30.seconds)

  // create and start our job service actor
  val service = system.actorOf(Props[JobServiceActor], "job-service")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = host, port = port)
}
