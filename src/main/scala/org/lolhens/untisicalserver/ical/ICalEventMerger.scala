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

        val grouped = sorted.foldLeft[List[List[Event]]](Nil) { (last, event) =>
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
        }

        merged
      }
}