package org.lolhens.untisicalserver.ical

import java.time.temporal.WeekFields
import java.time.{LocalDate, OffsetDateTime, ZoneOffset}

import org.lolhens.untisicalserver.ical.WeekOfYear._

import scala.annotation.tailrec

/**
  * Created by pierr on 24.03.2017.
  */
case class WeekOfYear(year: Int, week: Int) {
  private lazy val firstDayOfYear: LocalDate =
    LocalDate.MIN
      .withYear(year)
      .`with`(weekFields.weekOfYear(), 1)
      .`with`(weekFields.dayOfWeek(), 1)

  def startDate: LocalDate = firstDayOfYear.plusWeeks(week - 1)
  def midDate: LocalDate = startDate.plusDays(2)
  def endDate: LocalDate = startDate.plusWeeks(1)

  def +(weeks: Int): WeekOfYear = WeekOfYear(startDate.plusWeeks(weeks))
  def -(weeks: Int): WeekOfYear = this + -weeks
}

object WeekOfYear {
  implicit val ordering: Ordering[WeekOfYear] = Ordering.by(week => (week.year, week.week))

  private lazy val weekFields = WeekFields.ISO //WeekFields.of(Locale.getDefault())

  def apply(date: LocalDate): WeekOfYear =
    WeekOfYear(date.getYear, date.get(weekFields.weekOfYear()))

  def apply(year: Int, weekRange: Range): WeekRange = WeekRange(
    WeekOfYear(year, weekRange.start),
    WeekOfYear(year, if (weekRange.isInclusive) weekRange.end else weekRange.end - 1)
  )

  def now: WeekOfYear = WeekOfYear(OffsetDateTime.now(ZoneOffset.UTC).toLocalDate)

  case class WeekRange(start: WeekOfYear, end: WeekOfYear) {
    def toList: List[WeekOfYear] = {
      @tailrec
      def rec(week: WeekOfYear, weeks: List[WeekOfYear] = Nil): List[WeekOfYear] =
        if (week.startDate.isAfter(end.startDate)) weeks
        else rec(week + 1, week +: weeks)

      rec(start)
    }
  }

}
