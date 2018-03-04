logLevel := Level.Warn

resolvers ++= Seq(
  "lolhens-maven" at "http://artifactory.lolhens.de/artifactory/maven-public/",
  Resolver.url("lolhens-ivy", url("http://artifactory.lolhens.de/artifactory/ivy-public/"))(Resolver.ivyStylePatterns)
)

//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")

//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

addSbtPlugin("de.lolhens.sbt" % "sbt-classpath-jar" % "1.2.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

//addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.2")
