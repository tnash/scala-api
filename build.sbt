organization  := "com.fullcontact"

version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  val specsV = "2.3.13"

  Seq(
    // -- Logging --
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    // -- Spray --
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-caching" % sprayV,
    "io.spray"            %%  "spray-json"    % "1.3.2",
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    // -- Akka --
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    // -- testing --
    "org.specs2"          %%  "specs2-core"   % specsV % "test",
    "org.specs2"          %%  "specs2"        % specsV % "test",
    "org.scalatest"       %% "scalatest"      % "3.0.0" % "test",
    // -- caching --
    "com.github.cb372"    %  "scalacache-core_2.11" % "0.9.2",
    "com.github.cb372"    %% "scalacache-guava" % "0.9.2",
    "com.github.ben-manes.caffeine"           % "caffeine" % "2.3.3",
    // -- config --
    "com.typesafe"        % "config"          % "1.2.1"
  )
}
