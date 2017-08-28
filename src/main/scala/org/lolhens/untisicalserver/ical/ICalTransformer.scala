package org.lolhens.untisicalserver.ical

import akka.NotUsed
import akka.stream.scaladsl.Flow
import net.fortuna.ical4j.model._
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Summary
import org.lolhens.untisicalserver.data.SchoolClass
import org.lolhens.untisicalserver.util.Utils._
import scala.collection.JavaConverters._

/**
  * Created by pierr on 30.08.2016.
  */
object ICalTransformer {
  val flow: Flow[(SchoolClass, WeekOfYear, Calendar), (WeekOfYear, Calendar), NotUsed] =
    Flow[(SchoolClass, WeekOfYear, Calendar)]
      .map {
        case (schoolClass, week, calendar) =>
          val components = calendar.getComponents().asScala.toList.flatMap {
            case event: VEvent =>
              val lesson = Option(event.getSummary).map(_.getValue)
              val description = Option(event.getDescription.getValue)

              val (classNames, teacher) = {
                val split = description.get.split(" ")
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

              if (classNames.contains(schoolClass.className) && !lesson.contains("Förder")) List(event) else Nil

            case component =>
              List(component)
          }

          calendar.setComponents(components)
          (week, calendar)
      }
}