package org.lolhens.untisicalserver.data

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
}

object SchoolClass {
  val nixdorfFs15b = SchoolClass("nixdorf_bk_essen", "FS-15B", 183, Map(
    "feh" -> "Herr Fehlen",
    "krg" -> "Herr Krieg",
    "kri" -> "Frau Krigel",
    "meh" -> "Herr Mehl",
    "bol" -> "Herr Boll",
    "abl" -> "Frau Abel",
    "dib" -> "Frau Diblik",
    "win" -> "Frau Wingold"
  ), Map(
    "it1" -> "Linux",
    "it2" -> "Cisco Praxis",
    "it3" -> "Theorie",
    "aw1" -> "Programmieren",
    "aw2" -> "Datenbanken"
  ))
}