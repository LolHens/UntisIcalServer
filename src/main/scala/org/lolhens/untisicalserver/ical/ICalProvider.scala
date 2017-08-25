package org.lolhens.untisicalserver.ical

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import monix.execution.atomic.Atomic
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.data.SchoolClass

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ICalProvider(val schoolClass: SchoolClass, interval: FiniteDuration) {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val calendarCache = Atomic(Map.empty[WeekOfYear, Calendar])

  private lazy val cacheIn: Source[Seq[(WeekOfYear, Calendar)], NotUsed] =
    Source.repeat(schoolClass)
      .throttle(1, interval, 1, ThrottleMode.Shaping)
      .via(ICalReceiver.currentCalendars())
      .mapConcat {
        case (schoolClass, weeksAndcalendars) =>
          weeksAndcalendars
            .map(e => (schoolClass, e._1, e._2))
      }
      .via(ICalTransformer.flow)
      .flatMapConcat {
        case (week, calendar) =>
          Source.single(calendar)
            .map(_.getComponents.asScala.toList)
            .via(ICalEventMerger.flow)
            .map { newComponents =>
              calendar.setComponents(newComponents)
              (week, calendar)
            }
      }
      .groupedWithin(Int.MaxValue, 10.seconds)

  cacheIn
    .to(Sink.foreach { weeksAndCalendars =>
      calendarCache.transform { cache =>
        cache ++ weeksAndCalendars
      }
    })
    .run()

  private lazy val cacheOut =
    Source.repeat(())
      .map(_ => calendarCache.get.values.toList)
      .filter(_.nonEmpty)
      .via(ICalSplicer.flow)

  private lazy val outQueue = cacheOut.toMat(Sink.queue())(Keep.right).run()

  def apply(): Calendar = Await.result(outQueue.pull(), Duration.Inf).get

  // Flush the queue
  Source.tick(0.millis, 500.millis, ())
    .to(Sink.foreach(_ => apply()))
}
