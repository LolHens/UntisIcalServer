name := "UntisIcalServer"

mainClass := Some("org.lolhens.untisicalserver.Main")

lazy val settings = Seq(
  version := "1.3.0",

  scalaVersion := "2.11.8",

  resolvers += "Artifactory" at "http://lolhens.no-ip.org/artifactory/libs-release/",

  libraryDependencies ++= Seq(
    "org.apache.commons" % "commons-lang3" % "3.4",
    "commons-io" % "commons-io" % "2.5",
    "com.thoughtworks.xstream" % "xstream" % "1.4.9",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "org.scala-lang" % "scala-compiler" % "2.11.8",
    "org.scala-lang" % "scala-reflect" % "2.11.8",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
    "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
    "com.chuusai" %% "shapeless" % "2.3.1",
    "com.typesafe.akka" %% "akka-actor" % "2.4.9",
    "com.typesafe.akka" %% "akka-remote" % "2.4.9",
    "com.typesafe.akka" %% "akka-stream" % "2.4.9",
    "com.typesafe.akka" %% "akka-http-experimental" % "2.4.9",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.9",
    "com.typesafe.slick" %% "slick" % "3.1.1",
    "io.spray" %% "spray-json" % "1.3.2",
    "com.github.fommil" %% "spray-json-shapeless" % "1.2.0",
    "com.iheart" %% "ficus" % "1.2.0",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
    "org.mnode.ical4j" % "ical4j" % "2.0-beta1"
  ),

  scalacOptions ++= Seq("-Xmax-classfile-name", "254")
)

lazy val root = Project("untisicalserver", file("."))
  .enablePlugins(
    JavaAppPackaging,
    UniversalPlugin)
  .settings(settings: _*)
