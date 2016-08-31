package org.lolhens.untisicalserver

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import scala.concurrent.duration.Duration

/**
  * Created by pierr on 31.08.2016.
  */
class CachedValue[E](getValue: => E, timeout: Duration) {
  var value: Option[(E, LocalDateTime)] = None

  def invalid = value match {
    case Some((_, timestamp)) =>
      timestamp
        .plus(timeout.toMillis, ChronoUnit.MILLIS)
        .isAfter(LocalDateTime.now())

    case None =>
      true
  }

  def apply(): E = synchronized {
    if (invalid)
      value = Some((getValue, LocalDateTime.now()))

    value.get._1
  }
}

object CachedValue {
  def apply[E](getValue: => E, timeout: Duration) = new CachedValue[E](getValue, timeout)
}
