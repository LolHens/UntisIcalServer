package org.lolhens.untisicalserver.data

import org.lolhens.untisicalserver.ical.CachedICalProvider

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 30.08.2016.
  */
case class SchoolClass(school: String,
                       className: String,
                       classId: Int,
                       teacherNames: Map[String, String] = Map.empty,
                       lessonInfo: Map[String, String] = Map.empty) {
  def getTeacherName(name: String): Option[String] =
    teacherNames.get(name.toLowerCase)

  def getLessonInfo(lesson: String): Option[String] =
    lessonInfo.get(lesson.toLowerCase)

  lazy val iCalProvider = new CachedICalProvider(this, 5 minutes)
}

object SchoolClass {
  val hnbkTeachers = Map(
    "feh" -> "Herr Fehlen",
    "krg" -> "Herr Krieg",
    "kri" -> "Frau Krigel",
    "meh" -> "Herr Mehl",
    "bol" -> "Herr Boll",
    "abl" -> "Frau Abel",
    "dib" -> "Frau Diblik",
    "win" -> "Frau Wingold",
    "scm" -> "Herr Schmidt",
    "fel" -> "Frau Feller",
    "sci" -> "Herr Schiffers",
    "wor" -> "Frau Worat",
    "tri" -> "Frau Triebert Schreyer",
    "wen" -> "Frau Went"
  )

  val classes = Map(
    "fs15b" ->
      SchoolClass("nixdorf_bk_essen", "FS-15B", 183, hnbkTeachers, Map(
        "it1" -> "Linux",
        "it2" -> "Cisco Praxis",
        "it3" -> "Theorie",
        "aw1" -> "Programmieren",
        "aw2" -> "Datenbanken"
      )),

    "fs16b" ->
      SchoolClass("nixdorf_bk_essen", "FS-16B", 187, hnbkTeachers)
  )
}