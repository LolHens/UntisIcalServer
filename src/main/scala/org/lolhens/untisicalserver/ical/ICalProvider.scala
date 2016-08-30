package org.lolhens.untisicalserver.ical

import org.lolhens.untisicalserver.SchoolClass

class ICalProvider(val schoolClass: SchoolClass) {
  def apply() = {
    new ICalReceiver(schoolClass)
      .currentCalendars()
      .map(calendars =>
        new ICalSplicer(schoolClass)(calendars
          .map(ICalTransformer(_))))
      .onSuccess {
        case calendar =>
          println(calendar)
      }
  }
}
