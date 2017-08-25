package org.lolhens.untisicalserver.ical

import akka.NotUsed
import akka.stream.scaladsl.Flow
import net.fortuna.ical4j.model.component.{CalendarComponent, VEvent}

/**
  * Created by pierr on 01.09.2016.
  */
object ICalEventMerger {
  val flow: Flow[List[CalendarComponent], List[VEvent], NotUsed] =
    Flow[List[CalendarComponent]]
      .map { components =>
        val sorted = components.flatMap(_ match {
          case event: VEvent => List(event)
          case _ => Nil
        }).sortBy(_.getEndDate.getDate.toLocalDateTime)

        val grouped = sorted.foldLeft[List[List[VEvent]]](Nil) { (last, event) =>
          val startDate = event.getStartDate.getDate.toLocalDateTime

          val lastEvent: Option[VEvent] = last.lastOption.flatMap(_.lastOption)
          val lastEndDate = lastEvent.map(_.getEndDate.getDate.toLocalDateTime)

          lastEvent.map { lastEvent =>
            val lastEndDate = lastEvent.getEndDate.getDate.toLocalDateTime
            (lastEvent, lastEndDate)
          } match {
            case Some((lastEvent, lastEndDate))
              if lastEndDate == startDate
                && lastEvent.getSummary.getValue == event.getSummary.getValue
                && lastEvent.getLocation.getValue == event.getLocation.getValue
                && lastEvent.getDescription.getValue == event.getDescription.getValue =>
              last.dropRight(1) :+ (last.last :+ event)

            case _ =>
              last :+ List(event)
          }
        }

        val merged: List[VEvent] = grouped.flatMap { events =>
          events.headOption.map { event =>
            event.getEndDate.setValue(events.last.getEndDate.getValue)
            event
          }.toList
        }

        merged
      }
}