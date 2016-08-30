package org.lolhens.untisicalserver.ical

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyList
import org.lolhens.untisicalserver.SchoolClass

import scala.collection.JavaConversions._

/**
  * Created by pierr on 30.08.2016.
  */
class ICalTransformer(val schoolClass: SchoolClass) {
  def apply(calendar: Calendar): Calendar = {
    new Calendar(
      calendar.getProperties(), {
        val propertyList = new PropertyList()


          calendar.getComponents().flatMap { component =>
            val summary = component.getProperty(Property.SUMMARY).getValue

            val (classNames, teacher) = {
              val description = component.getProperty(Property.DESCRIPTION).getValue
              val split = description.split(" ")
              (split.dropRight(1), split.last)
            }

            if (classNames.contains(schoolClass.className)) {
              component.getProperty(Property.SUMMARY).setValue(s"$summary $teacher")
              component.getProperty(Property.DESCRIPTION).setValue("")

              List(component)
            } else
              Nil
          }

        propertyList
      })
  }
}
