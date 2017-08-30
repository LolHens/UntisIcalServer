name := "UntisIcalServer"

mainClass := Some("org.lolhens.untisicalserver.Main")

lazy val settings = Seq(
  version := "1.7.9",

  scalaVersion := "2.12.3",

  externalResolvers := Seq(
    Resolver.defaultLocal,
    "artifactory-maven" at "http://lolhens.no-ip.org/artifactory/maven-public/",
    Resolver.url("artifactory-ivy", url("http://lolhens.no-ip.org/artifactory/ivy-public/"))(Resolver.ivyStylePatterns)
  ),

  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "org.typelevel" %% "cats" % "0.9.0",
    "com.chuusai" %% "shapeless" % "2.3.2",
    "com.github.mpilquist" %% "simulacrum" % "0.11.0",
    "io.monix" %% "monix" % "2.3.0",
    "io.monix" %% "monix-cats" % "2.3.0",
    "com.typesafe.akka" %% "akka-actor" % "2.5.4",
    "com.typesafe.akka" %% "akka-remote" % "2.5.4",
    "com.typesafe.akka" %% "akka-stream" % "2.5.4",
    "com.typesafe.akka" %% "akka-http" % "10.0.9",
    "io.spray" %% "spray-json" % "1.3.3",
    "com.github.fommil" %% "spray-json-shapeless" % "1.4.0",
    "net.databinder.dispatch" %% "dispatch-core" % "0.12.3",
    "org.mnode.ical4j" % "ical4j" % "2.0.4",
    "com.github.pureconfig" %% "pureconfig" % "0.8.0",
    "com.google.api-client" % "google-api-client" % "1.22.0",
    "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0",
    "com.google.apis" % "google-api-services-calendar" % "v3-rev254-1.22.0"
  ),

  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),

  scalacOptions ++= Seq("-Xmax-classfile-name", "127")
)

def packageConfFolder(confName: String) = Seq(
  mappings in(Compile, packageBin) := (mappings in(Compile, packageBin)).value.filterNot(_._2 == confName),
  mappings in Universal += ((resourceDirectory in Compile).value / confName -> s"conf/$confName")
)

lazy val root = Project("untisicalserver", file("."))
  .enablePlugins(
    JavaAppPackaging,
    UniversalPlugin,
    SbtClasspathJarPlugin)
  .settings(settings: _*)
  .settings(packageConfFolder("application.conf"): _*)
