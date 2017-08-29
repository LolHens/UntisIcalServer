package org.lolhens.untisicalserver.data.config

import org.lolhens.untisicalserver.ical.ICalProvider

import scala.concurrent.duration._

case class SchoolClass(id: Int,
                       ref: String,
                       name: String,
                       lessons: Map[String, Lesson]) {
  private[config] var _school: School = _

  def school: School = _school

  lazy val iCalProvider = new ICalProvider(this, 2.minutes)

  def getTeacherName(name: String): Option[String] =
    school.teachers.get(name.toLowerCase)

  def getLessonInfo(lesson: String): Option[String] =
    lessons.get(lesson.toLowerCase)
      .flatMap(lesson =>
        Some(lesson.description).filter(_.trim.nonEmpty)
      )

  def isLessonHidden(lesson: String): Boolean =
    lessons.get(lesson.toLowerCase).exists(_.hide)
}
