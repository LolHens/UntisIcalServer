package org.lolhens.untisicalserver.ical

import net.fortuna.ical4j.model._
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Summary
import org.lolhens.untisicalserver.data.SchoolClass

import scala.collection.JavaConversions._

/**
  * Created by pierr on 30.08.2016.
  */
object ICalTransformer {
  def apply(schoolClass: SchoolClass, calendar: Calendar): Calendar = {
    val components =
      calendar.getComponents().toList
        .flatMap {
          case event: VEvent =>
            val lesson = Option(event.getSummary).map(_.getValue)
            val description = event.getDescription.getValue

            val (classNames, teacher) = {
              val split = description.split(" ")
              (split.dropRight(1), split.last)
            }

            lesson match {
              case Some(lesson) =>
                event.getSummary.setValue(s"$lesson")

              case None =>
                event.addProperty(new Summary("???"))
            }

            event.getDescription.setValue(
              s"${
                lesson.map { lesson =>
                  s"""$lesson${
                    schoolClass.getLessonInfo(lesson).map(e => s" $e").getOrElse("")
                  }\n"""
                }.getOrElse("")
              }${
                schoolClass.getTeacherName(teacher).getOrElse(teacher)
              }"
            )

            if (classNames.contains(schoolClass.className) && lesson != "Förder") List(event) else Nil

          case component =>
            List(component)
        }

    calendar.setComponents(components)
    calendar
  }
}