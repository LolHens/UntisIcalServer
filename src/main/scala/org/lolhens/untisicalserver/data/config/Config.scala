package org.lolhens.untisicalserver.data.config

import java.io.File
import java.util.{Locale, TimeZone}

import com.typesafe.config.ConfigFactory
import pureconfig._

case class Config(locale: Option[String],
                  timezone: Option[String],
                  schools: List[School]) {
  def getSchoolClass(schoolRef: String, classRef: String): Option[SchoolClass] =
    schools.filter(_.ref.equalsIgnoreCase(schoolRef)).flatMap(_.classes).find(_.ref.equalsIgnoreCase(classRef))

  for (locale <- locale)
    Locale.setDefault(Locale.forLanguageTag(locale.replaceAllLiterally("_", "-")))

  for (timezone <- timezone)
    TimeZone.setDefault(TimeZone.getTimeZone(timezone))
}

object Config {
  private def config = ConfigFactory.load()
    .withFallback(ConfigFactory.parseFile(new File("../conf/application.conf")))

  lazy val load: Config = loadConfig[Config](config, "icalserver") match {
    case Right(config) => config
    case Left(f) => throw new RuntimeException("\n" + f.toList.mkString("\n"))
  }
}
