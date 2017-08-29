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

        def merge(before: Event, after: Event): Option[Event] = {
          if (before.end == after.start &&
            before.summary == after.summary &&
            before.description == after.description &&
            before.location == after.location)
            Some(before.copy(end = after.end))
          else
            None
        }

        val merged = sorted.foldLeft(List.empty[Event]) { (lastEvents, event) =>
          lastEvents.lastOption.flatMap { lastEvent =>
            merge(lastEvent, event)
          }.map(lastEvents.dropRight(1) :+ _)
            .getOrElse(lastEvents :+ event)
        }

        merged
      }
}