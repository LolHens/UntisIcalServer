package org.lolhens.untisicalserver

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.{Calendar, Component, ComponentList, Property}

import scala.collection.JavaConverters._


/**
  * Created by pierr on 01.09.2016.
  */
package object ical {
  def ComponentList[E <: Component](components: List[E]): ComponentList[E] = {
    val componentList = new ComponentList[E]()
    componentList.addAll(components.asJava)
    componentList
  }

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  implicit class RichDate(val date: Date) extends AnyVal {
    def toLocalDateTime: LocalDateTime = LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)
  }

  implicit class RichCalendar(val calendar: Calendar) extends AnyVal {
    def setComponents(components: List[CalendarComponent]): Unit = {
      calendar.getComponents.clear()
      calendar.getComponents.addAll(components.asJava)
    }
  }

  implicit class RichCalendarComponent(val component: CalendarComponent) extends AnyVal {
    def removeProperty(name: String): Unit =
      component.getProperties().removeIf((t: Property) => t.getName.equalsIgnoreCase(name))

    def addProperty(property: Property): Unit =
      component.getProperties.add(property)
  }

}
