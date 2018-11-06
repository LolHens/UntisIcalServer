package org.lolhens.untisicalserver.http.server

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import monix.eval.Task
import monix.execution.Scheduler
import org.lolhens.untisicalserver.data.config.Config

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Created by pierr on 31.08.2016.
  */
class ICalServer(config: Config) {
  implicit lazy val system = ActorSystem()
  implicit lazy val materializer = ActorMaterializer()

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().bind(interface = "0.0.0.0", port = 8080)

  object ClassId {
    def unapply(string: String): Option[(String, String)] = {
      val prefix = "/ical/"
      val postfix = ".ics"

      if (string.startsWith(prefix)
        && string.endsWith(postfix)) {
        val schoolRef :: classRef :: _ = string.drop(prefix.length).dropRight(postfix.length).split("/", -1).toList :+ ""
        Some((schoolRef, classRef))
      } else
        None
    }
  }

  val unknown = HttpResponse(404, entity = "Unknown resource!")

  def bindingFuture(implicit scheduler: Scheduler): Future[Http.ServerBinding] =
    serverSource.to(Sink.foreach { connection =>
      connection.handleWith(Flow[HttpRequest].collect {
        case r@HttpRequest(GET, Uri.Path(ClassId(schoolRef, classRef)), _, _, _) if config.getSchoolClass(schoolRef, classRef).nonEmpty =>
          val schoolClass = config.getSchoolClass(schoolRef, classRef).get
          val calendar = schoolClass.calendars.calendar.runSyncUnsafe(Duration.Inf)
          val response = calendar.icalString.getBytes(StandardCharsets.UTF_8)

          r.discardEntityBytes()
          HttpResponse(entity = HttpEntity(ContentTypes.`application/octet-stream`, response))
        case r: HttpRequest =>
          r.discardEntityBytes() // important to drain incoming HTTP Entity stream
          unknown
      })
    }).run()

  val start: Task[Unit] = Task.deferFutureAction[Http.ServerBinding](bindingFuture(_)).flatMap { binding =>
    Task.never.doOnCancel(Task.deferFuture(binding.unbind()).map(_ => ()))
  }
}
