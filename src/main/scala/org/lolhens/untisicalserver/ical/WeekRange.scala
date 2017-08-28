package org.lolhens.untisicalserver.ical

import scala.annotation.tailrec

/**
  * Created by pierr on 24.03.2017.
  */
case class WeekRange(start: WeekOfYear, end: WeekOfYear) {
  def toList: List[WeekOfYear] = {
    @tailrec
    def rec(week: WeekOfYear, weeks: List[WeekOfYear]): List[WeekOfYear] =
      if (week.localDateMin.isAfter(end.localDateMin)) weeks
      else rec(week + 1, week +: weeks)

    val r = rec(start, Nil)
    println(r.size + " " + r.head)
    r
  }
}
