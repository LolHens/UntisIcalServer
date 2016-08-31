package org.lolhens.untisicalserver.ical

import java.util.function.Predicate

import net.fortuna.ical4j.model._
import net.fortuna.ical4j.model.component.CalendarComponent
import org.lolhens.untisicalserver.SchoolClass

import scala.collection.JavaConversions._

/**
  * Created by pierr on 30.08.2016.
  */
object ICalTransformer {
  def apply(schoolClass: SchoolClass, calendar: Calendar): Calendar = {
    def removeProperty(component: CalendarComponent, name: String) =
      component.getProperties().removeIf(new Predicate[Property]() {
        override def test(t: Property): Boolean = t.getName.equalsIgnoreCase(name)
      })

    val components = ComponentList(
      calendar.getComponents().toList
        .flatMap { component =>
          val summary = component.getProperty(Property.SUMMARY).getValue

          val (classNames, teacher) = {
            val description = component.getProperty(Property.DESCRIPTION).getValue
            val split = description.split(" ")
            (split.dropRight(1), split.last)
          }

          if (classNames.contains(schoolClass.className)) {
            component.getProperty(Property.SUMMARY).setValue(s"$summary $teacher")
            removeProperty(component, Property.DESCRIPTION)

            List(component)
          } else
            Nil
        }
    )

    new Calendar(
      calendar.getProperties(),
      components
    )
  }

  def ComponentList[E <: Component](components: List[E]) = {
    val componentList = new ComponentList[E]()
    componentList.addAll(components)
    componentList
  }
}