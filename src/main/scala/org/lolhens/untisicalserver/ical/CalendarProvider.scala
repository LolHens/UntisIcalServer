package org.lolhens.untisicalserver.ical

import monix.eval.Task
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.config.SchoolClass
import org.lolhens.untisicalserver.data.{Calendar, Event}
import org.lolhens.untisicalserver.ical.WeekOfYear.WeekRange
import org.lolhens.untisicalserver.util.Utils._

import scala.concurrent.duration._
import scala.language.postfixOps

case class CalendarProvider(schoolClass: SchoolClass) {
  def receiveWeek(schoolClass: SchoolClass,
                  week: WeekOfYear): Task[Option[Calendar]] = {
    def retryReceiveWeek(retries: Int): Task[Calendar] =
      CalendarRequester.request(schoolClass, week)
        .onErrorRecoverWith {
          case err =>
            println(s"Retrying $retries times after: $err") //TODO
            //err.printStackTrace()
            if (retries <= 0) Task.raiseError(err)
            else retryReceiveWeek(retries - 1).delayExecution(5.seconds)
        }

    retryReceiveWeek(5)
      .materialize
      .map(_.toOption)
  }

  def receiveWeekRange(schoolClass: SchoolClass,
                       weekRange: WeekRange): Observable[(WeekOfYear, Calendar)] =
    Observable.fromIterable(weekRange.toList)
      .mapParallelUnordered(8) { week =>
        for {
          calendarOption <- receiveWeek(schoolClass, week)
        } yield for {
          calendar <- calendarOption
        } yield
          week -> calendar
      }
      .flatMap(e => Observable.fromIterable(e.toSeq))
      .switchIfEmpty(Observable.raiseError(new RuntimeException("No calendar fetched!")))

  def receive(schoolClass: SchoolClass,
              back: Int = 20,
              forward: Int = 10): Observable[(WeekOfYear, Calendar)] =
    receiveWeekRange(schoolClass, WeekRange(WeekOfYear.now - back, WeekOfYear.now + forward))

  def transform(schoolClass: SchoolClass,
                week: WeekOfYear,
                calendar: Calendar) = Task {
    val newEvents =
      calendar.events.flatMap { event =>
        val lesson = event.summary
        val description = event.description

        val Seq(classNames, teacher, _*) =
          description.split(" ", -1).toList :+ ""

        val newSummary =
          if (lesson.isEmpty) "???"
          else lesson

        val teacherName =
          schoolClass
            .getTeacherName(teacher)
            .getOrElse(teacher)

        val lessonInfo =
          schoolClass
            .getLessonInfo(lesson)
            .map(e => s" $e")
            .getOrElse("")

        val newDescription =
          s"$lesson$lessonInfo\n$teacherName"

        if (classNames.contains(schoolClass.name) && !schoolClass.isLessonHidden(lesson))
          List(event.copy(
            summary = newSummary,
            description = newDescription
          ))
        else Nil
      }

    calendar.copy(events = newEvents)
  }

  def mergeEvents(events: Seq[Event]): Task[List[Event]] = Task {
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

  def mergeCalendarEvents(calendar: Calendar): Task[Calendar] =
    for {
      mergedEvents <- mergeEvents(calendar.events)
    } yield
      calendar.copy(events = mergedEvents)

  val calendars: Observable[(WeekOfYear, Calendar)] =
    for {
      (week, calendar) <- receive(schoolClass)
      calendar <- Observable.fromTask(transform(schoolClass, week, calendar))
      calendar <- Observable.fromTask(mergeCalendarEvents(calendar))
    } yield
      week -> calendar
}
