package org.lolhens.untisicalserver.data

/**
  * Created by pierr on 30.08.2016.
  */
case class SchoolClass(school: String,
                       className: String,
                       classId: Int,
                       teacherProvider: Option[TeacherProvider] = None)

object SchoolClass {
  val nixdorfFs15b = SchoolClass("nixdorf_bk_essen", "FS-15B", 183, Some(TeacherProvider.HNBK))
}