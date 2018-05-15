name := "UntisIcalServer"

mainClass := Some("org.lolhens.untisicalserver.Main")

lazy val settings = Seq(
  version := "2.0.0",

  scalaVersion := "2.12.4",

  resolvers ++= Seq(
    "lolhens-maven" at "http://artifactory.lolhens.de/artifactory/maven-public/",
    Resolver.url("lolhens-ivy", url("http://artifactory.lolhens.de/artifactory/ivy-public/"))(Resolver.ivyStylePatterns)
  ),

  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "org.typelevel" %% "cats-core" % "1.1.0",
    "io.monix" %% "monix" % "3.0.0-RC1",
    "com.typesafe.akka" %% "akka-stream" % "2.5.12",
    "com.typesafe.akka" %% "akka-http" % "10.1.1",
    "io.spray" %% "spray-json" % "1.3.4",
    "net.databinder.dispatch" %% "dispatch-core" % "0.12.3",
    "org.mnode.ical4j" % "ical4j" % "2.2.0",
    "com.github.pureconfig" %% "pureconfig" % "0.9.1",
    "com.google.api-client" % "google-api-client" % "1.23.0",
    "com.google.oauth-client" % "google-oauth-client-jetty" % "1.23.0",
    "com.google.apis" % "google-api-services-calendar" % "v3-rev317-1.22.0"
  ),

  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),

  scalacOptions ++= Seq("-Xmax-classfile-name", "127"),

  assemblyOption in assembly := {
    def universalScript(shellCommands: String,
                        cmdCommands: String,
                        shebang: Boolean): String = {
      Seq(
        if (shebang) "#!/usr/bin/env sh" else "",
        "@ 2>/dev/null # 2>nul & echo off & goto BOF\r",
        ":",
        shellCommands.replaceAll("\r\n|\n", "\n"),
        "exit",
        Seq(
          "",
          ":BOF",
          cmdCommands.replaceAll("\r\n|\n", "\r\n"),
          "exit /B %errorlevel%",
          ""
        ).mkString("\r\n")
      ).filterNot(_.isEmpty).mkString("\n")
    }

    def defaultUniversalScript(javaOpts: Seq[String] = Seq.empty, shebang: Boolean = true): Seq[String] = {
      val javaOptsString = javaOpts.map(_ + " ").mkString
      Seq(universalScript(
        shellCommands = s"""exec java -jar $javaOptsString$$JAVA_OPTS "$$0" "$$@"""",
        cmdCommands = s"""java -jar $javaOptsString%JAVA_OPTS% "%~dpnx0" %*""",
        shebang = shebang
      ))
    }

    (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultUniversalScript()))
  },

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
