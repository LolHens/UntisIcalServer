package org.lolhens.untisicalserver

import java.time.{LocalDateTime, ZoneId}
import java.util.Date
import java.util.function.Predicate

import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.{Calendar, Component, ComponentList, Property}

import scala.collection.JavaConversions._


/**
  * Created by pierr on 01.09.2016.
  */
package object ical {
  def ComponentList[E <: Component](components: List[E]) = {
    val componentList = new ComponentList[E]()
    componentList.addAll(components)
    componentList
  }

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  implicit class RichDate(val date: Date) extends AnyVal {
    def toLocalDateTime = LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)
  }

  implicit class RichCalendar(val calendar: Calendar) extends AnyVal {
    def setComponents(components: List[CalendarComponent]): Unit = {
      calendar.getComponents.clear()
      calendar.getComponents.addAll(components)
    }

    def removeProperty(component: CalendarComponent, name: String) =
      component.getProperties().removeIf(new Predicate[Property]() {
        override def test(t: Property): Boolean = t.getName.equalsIgnoreCase(name)
      })
  }

}
