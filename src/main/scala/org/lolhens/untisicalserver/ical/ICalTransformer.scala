package org.lolhens.untisicalserver.ical

import akka.NotUsed
import akka.stream.scaladsl.Flow
import monix.eval.Task
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass

/**
  * Created by pierr on 30.08.2016.
  */
object ICalTransformer {
  val flow: Flow[(SchoolClass, WeekOfYear, Calendar), (WeekOfYear, Calendar), NotUsed] =
    Flow[(SchoolClass, WeekOfYear, Calendar)].map {
      case (schoolClass, week, calendar) =>
        val newEvents =
          calendar.events.flatMap { event =>
            val lesson = event.summary
            val description = event.description

            val classNames :: teacher :: _ = description.split(" ", -1).toList :+ ""

            val newSummary = lesson match {
              case "" => "???"
              case lesson => lesson
            }

            val teacherName = schoolClass.getTeacherName(teacher).getOrElse(teacher)

            val lessonInfo = schoolClass.getLessonInfo(lesson).map(e => s" $e").getOrElse("")

            val newDescription = s"$lesson$lessonInfo\n$teacherName"

            if (classNames.contains(schoolClass.name) &&
              !schoolClass.isLessonHidden(lesson)) List(event.copy(
              summary = newSummary,
              description = newDescription
            ))
            else Nil
          }

        (week, calendar.copy(events = newEvents))
    }

  def task(schoolClass: SchoolClass,
           week: WeekOfYear,
           calendar: Calendar) = Task {
    val newEvents =
      calendar.events.flatMap { event =>
        val lesson = event.summary
        val description = event.description

        val Seq(classNames, teacher, _*) = description.split(" ", -1).toList :+ ""

        val newSummary = lesson match {
          case "" => "???"
          case lesson => lesson
        }

        val teacherName = schoolClass.getTeacherName(teacher).getOrElse(teacher)

        val lessonInfo = schoolClass.getLessonInfo(lesson).map(e => s" $e").getOrElse("")

        val newDescription = s"$lesson$lessonInfo\n$teacherName"

        if (classNames.contains(schoolClass.name) &&
          !schoolClass.isLessonHidden(lesson)) List(event.copy(
          summary = newSummary,
          description = newDescription
        ))
        else Nil
      }

    calendar.copy(events = newEvents)
  }
}