package org.lolhens.untisicalserver.data

/**
  * Created by pierr on 01.09.2016.
  */
abstract class TeacherProvider {
  val names: Map[String, String]

  def getRealName(name: String): Option[String] = names.get(name.toLowerCase)
}

object TeacherProvider {

  object HNBK extends TeacherProvider {
    val names = Map(
      "feh" -> "Fehlen",
      "krg" -> "Krieg",
      "kri" -> "Krigel",
      "meh" -> "Mehl",
      "bol" -> "Boll",
      "abl" -> "Abel",
      "dib" -> "Diblik",
      "win" -> "Wingold"
    )
  }

}