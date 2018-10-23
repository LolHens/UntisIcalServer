package org.lolhens.untisicalserver.data.config

import java.io.File
import java.nio.file.{Path, Paths}
import java.util.{Locale, TimeZone}

import com.typesafe.config.ConfigFactory
import pureconfig._

case class Config(locale: Option[String],
                  timezone: Option[String],
                  googleservice: Boolean,
                  schools: List[School]) {
  def getSchoolClass(schoolRef: String, classRef: String): Option[SchoolClass] =
    schools.filter(_.ref.equalsIgnoreCase(schoolRef)).flatMap(_.classes).find(_.ref.equalsIgnoreCase(classRef))

  for (locale <- locale)
    Locale.setDefault(Locale.forLanguageTag(locale.replaceAllLiterally("_", "-")))

  for (timezone <- timezone)
    TimeZone.setDefault(TimeZone.getTimeZone(timezone))
}

object Config {
  def jarPath: Path = Paths.get(new File(getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath).getPath)

  def primaryConf: Path = jarPath.resolveSibling("application.conf")
  def secondaryConf: Path = jarPath.getParent.resolveSibling("conf").resolve("application.conf")

  def config = ConfigFactory.parseFile(primaryConf.toFile)
    .withFallback(ConfigFactory.parseFile(secondaryConf.toFile)) // TODO: deprecate?
    .withFallback(ConfigFactory.load())

  lazy val load: Config = loadConfig[Config](config, "icalserver") match {
    case Right(config) => config
    case Left(f) => throw new RuntimeException("\n" + f.toList.mkString("\n"))
  }
}
