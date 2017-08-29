package org.lolhens.untisicalserver.data.config

import java.io.File

import com.typesafe.config.ConfigFactory
import pureconfig._

case class Config(schools: List[School]) {
  def getSchoolClass(schoolRef: String, classRef: String): Option[SchoolClass] =
    schools.filter(_.ref.equalsIgnoreCase(schoolRef)).flatMap(_.classes).find(_.ref.equalsIgnoreCase(classRef))
}

object Config {
  private def config = ConfigFactory.load()
    .withFallback(ConfigFactory.parseFile(new File("../conf/application.conf")))

  lazy val load: Config = loadConfig[Config](config, "icalserver").right.get
}
