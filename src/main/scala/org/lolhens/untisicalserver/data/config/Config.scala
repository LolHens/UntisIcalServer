package org.lolhens.untisicalserver.data.config

import java.io.File
import java.util.TimeZone

import com.typesafe.config.ConfigFactory
import pureconfig._

case class Config(timezone: Option[String], schools: List[School]) {
  def getSchoolClass(schoolRef: String, classRef: String): Option[SchoolClass] =
    schools.filter(_.ref.equalsIgnoreCase(schoolRef)).flatMap(_.classes).find(_.ref.equalsIgnoreCase(classRef))

  timezone.foreach(zone => TimeZone.setDefault(TimeZone.getTimeZone(zone)))
}

object Config {
  private def config = ConfigFactory.load()
    .withFallback(ConfigFactory.parseFile(new File("../conf/application.conf")))

  lazy val load: Config = loadConfig[Config](config, "icalserver") match {
    case Right(config) => config
    case Left(f) => throw new RuntimeException("\n" + f.toList.mkString("\n"))
  }
}
