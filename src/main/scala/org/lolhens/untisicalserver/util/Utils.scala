package org.lolhens.untisicalserver.util

import java.time._
import java.util.Date

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

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  private[util] def zoneOffset: ZoneOffset = OffsetDateTime.now().getOffset

  implicit class RichDate(val date: Date) extends AnyVal {
    def toLocalDateTime: LocalDateTime = LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)
  }

  implicit class RichLocalDateTime(val localDateTime: LocalDateTime) extends AnyVal {
    def toDate: Date = Date.from(localDateTime.toInstant(zoneOffset))
  }

  implicit class RichLocalDate(val localDate: LocalDate) extends AnyVal {
    def midnight: LocalDateTime = LocalDateTime.of(localDate, LocalTime.MIDNIGHT)
  }

  implicit class RichCalendar(val calendar: Calendar) extends AnyVal {
    def setComponents(components: List[CalendarComponent]): Unit = {
      calendar.getComponents.clear()
      calendar.getComponents.addAll(components.asJava)
    }

    def events: List[VEvent] = calendar.getComponents().asScala.toList.collect {
      case event: VEvent => event
    }
  }

  implicit class RichCalendarComponent(val component: CalendarComponent) extends AnyVal {
    def removeProperty(name: String): Unit =
      component.getProperties().removeIf((t: Property) => t.getName.equalsIgnoreCase(name))

    def addProperty(property: Property): Unit =
      component.getProperties.add(property)
  }

}
