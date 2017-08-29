package org.lolhens.untisicalserver.data.config

case class School(id: String,
                  ref: String,
                  teachers: Map[String, String],
                  classes: List[SchoolClass]) {
  classes.foreach(_._school = this)
}
