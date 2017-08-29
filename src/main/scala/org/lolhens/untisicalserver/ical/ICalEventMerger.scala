package org.lolhens.untisicalserver.ical

import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.lolhens.untisicalserver.data.Event
import org.lolhens.untisicalserver.util.Utils._

/**
  * Created by pierr on 01.09.2016.
  */
object ICalEventMerger {
  val flow: Flow[List[Event], List[Event], NotUsed] =
    Flow[List[Event]]
      .map { events =>
        val sorted = events.sortBy(_.end)

        /*val grouped = sorted.foldLeft[List[List[Event]]](Nil) { (last, event) =>
          val startDate = event.start

          val lastEvent: Option[Event] = last.lastOption.flatMap(_.lastOption)
          val lastEndDate = lastEvent.map(_.end)

          lastEvent.map { lastEvent =>
            val lastEndDate = lastEvent.end
            (lastEvent, lastEndDate)
          } match {
            case Some((lastEvent, lastEndDate))
              if lastEndDate == startDate
                && lastEvent.summary == event.summary
                && lastEvent.location == event.location
                && lastEvent.description == event.description =>
              last.dropRight(1) :+ (last.last :+ event)

            case _ =>
              last :+ List(event)
          }
        }

        val merged: List[Event] = grouped.flatMap { events =>
          events.headOption.map(_.copy(end = events.last.end)).toList
        }*/

        def merge(before: Event, after: Event): Option[Event] = {
          if (before.end == after.start &&
            before.summary == after.summary &&
            before.description == after.description &&
            before.location == after.location)
            Some(before.copy(end = after.end))
          else
            None
        }

        if (sorted.exists(_.start.toString.startsWith("2017-08-30")))
          println(sorted.map(e => s"${e.summary} ${e.start} ${e.end}"))

        val merged = sorted.foldLeft(List.empty[Event]) { (lastEvents, event) =>
          lastEvents.lastOption.flatMap { lastEvent =>
            merge(lastEvent, event)
          }.map(lastEvents.dropRight(1) :+ _)
            .getOrElse(lastEvents :+ event)
        }

        merged
      }
}