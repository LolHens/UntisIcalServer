package org.lolhens.untisicalserver.util

import java.time.{LocalDate, LocalTime, OffsetDateTime, ZoneOffset}

import ch.qos.logback.classic.{Level, Logger}
import net.fortuna.ical4j.model.component.{CalendarComponent, VEvent}
import net.fortuna.ical4j.model.{Calendar, Component, ComponentList, Property}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object Utils {
  lazy val setLogLevel: Unit = {
    val logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    logger.setLevel(Level.INFO)
  }

  def ComponentList[E <: Component](components: List[E]): ComponentList[E] = {
    val componentList = new ComponentList[E]()
    componentList.addAll(components.asJava)
    componentList
  }

  //implicit val offsetDateTimeOrdering: Ordering[OffsetDateTime] = Ordering.fromLessThan(_ isBefore _)

  def zoneOffset: ZoneOffset = OffsetDateTime.now().getOffset

  implicit class LocalDateOps(val date: LocalDate) extends AnyVal {
    def dayStart: OffsetDateTime = OffsetDateTime.of(date, LocalTime.MIDNIGHT, ZoneOffset.UTC)

    def dayEnd: OffsetDateTime = dayStart.plusDays(1)
  }

  implicit class CalendarOps(val calendar: Calendar) extends AnyVal {
    def setComponents(components: List[CalendarComponent]): Unit = {
      calendar.getComponents.clear()
      calendar.getComponents.addAll(components.asJava)
    }

    def events: List[VEvent] = calendar.getComponents().asScala.toList.collect {
      case event: VEvent => event
    }
  }

  implicit class CalendarComponentOps(val component: CalendarComponent) extends AnyVal {
    def removeProperty(name: String): Unit =
      component.getProperties().removeIf((t: Property) => t.getName.equalsIgnoreCase(name))

    def addProperty(property: Property): Unit =
      component.getProperties.add(property)
  }

  //private lazy val parallelism = Runtime.getRuntime.availableProcessors() + 2

  /*def parallel[T](tasks: List[Task[T]], unordered: Boolean = false): Task[List[T]] = Task.sequence {
    tasks.sliding(parallelism, parallelism).map(e =>
      if (unordered) Task.gatherUnordered(e)
      else Task.gather(e)
    )
  }.map(_.toList.flatten)*/
}
