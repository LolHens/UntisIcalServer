package org.lolhens.untisicalserver.ical

import scala.annotation.tailrec

/**
  * Created by pierr on 24.03.2017.
  */
case class WeekRange(start: WeekOfYear, end: WeekOfYear) {
  def toList: List[WeekOfYear] = {
    @tailrec
    def rec(week: WeekOfYear, weeks: List[WeekOfYear] = Nil): List[WeekOfYear] =
      if (week.localDateMin.isAfter(end.localDateMin)) weeks
      else rec(week + 1, week +: weeks)

    rec(start)
  }
}
