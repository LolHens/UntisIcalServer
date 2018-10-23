name := "UntisIcalServer"

mainClass := Some("org.lolhens.untisicalserver.Main")

lazy val settings = Seq(
  version := "2.0.6-SNAPSHOT",

  scalaVersion := "2.12.4",

  resolvers ++= Seq(
    "lolhens-maven" at "http://artifactory.lolhens.de/artifactory/maven-public/",
    Resolver.url("lolhens-ivy", url("http://artifactory.lolhens.de/artifactory/ivy-public/"))(Resolver.ivyStylePatterns)
  ),

  libraryDependencies ++= Seq(
    //"org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "org.typelevel" %% "cats-core" % "1.4.0",
    "io.monix" %% "monix" % "3.0.0-RC1",
    "com.typesafe.akka" %% "akka-stream" % "2.5.17",
    "com.typesafe.akka" %% "akka-http" % "10.1.5",
    "io.spray" %% "spray-json" % "1.3.4",
    "net.databinder.dispatch" %% "dispatch-core" % "0.12.3",
    "org.mnode.ical4j" % "ical4j" % "2.2.0",
    "com.github.pureconfig" %% "pureconfig" % "0.9.1",
    "com.google.api-client" % "google-api-client" % "1.26.0",
    "com.google.oauth-client" % "google-oauth-client-jetty" % "1.26.0",
    "com.google.apis" % "google-api-services-calendar" % "v3-rev355-1.25.0"
  ),

  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  //addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),

  scalacOptions ++= Seq("-Xmax-classfile-name", "127"),

  assemblyOption in assembly := (assemblyOption in assembly).value
    .copy(prependShellScript = Some(AssemblyPlugin.defaultUniversalScript(shebang = true))),

  assemblyJarName in assembly := s"${name.value}-${version.value}.sh.bat"
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
