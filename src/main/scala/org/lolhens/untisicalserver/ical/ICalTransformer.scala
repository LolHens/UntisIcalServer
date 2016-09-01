package org.lolhens.untisicalserver.ical

import net.fortuna.ical4j.model._
import net.fortuna.ical4j.model.component.VEvent
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
            val summary = event.getSummary.getValue
            val description = event.getDescription.getValue

            val (classNames, teacher) = {
              val split = description.split(" ")
              (split.dropRight(1), split.last)
            }

            event.getSummary.setValue(s"$summary $teacher")
            event.getDescription.setValue(schoolClass.teacherProvider.flatMap(_.getRealName(teacher)).getOrElse(""))

            if (classNames.contains(schoolClass.className) && summary != "Förder") List(event) else Nil

          case component =>
            List(component)
        }

    calendar.setComponents(components)
    calendar
  }
}