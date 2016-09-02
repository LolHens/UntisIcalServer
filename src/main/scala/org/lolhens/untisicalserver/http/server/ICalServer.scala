package org.lolhens.untisicalserver.http.server

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.lolhens.untisicalserver.data.SchoolClass

import scala.concurrent.Future

/**
  * Created by pierr on 31.08.2016.
  */
class ICalServer() {
  def start() = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().bind(interface = "0.0.0.0", port = 8080)

    object ClassId {
      def unapply(string: String): Option[(String)] = {
        val prefix = "/ical/"
        val postfix = ".ics"

        if (string.startsWith(prefix)
          && string.endsWith(postfix))
          Some(string.drop(prefix.size).dropRight(postfix.size))
        else
          None
      }
    }

    val bindingFuture: Future[Http.ServerBinding] =
      serverSource.to(Sink.foreach { connection =>
        connection.handleWith(Flow[HttpRequest].collect {
          case HttpRequest(GET, Uri.Path(ClassId(classId)), _, _, _) if SchoolClass.classes.contains(classId) =>
            val schoolClass = SchoolClass.classes(classId)
            val response = schoolClass.iCalProvider().toString.getBytes(StandardCharsets.UTF_8)

            HttpResponse(entity = HttpEntity(ContentTypes.`application/octet-stream`, response))
          case r: HttpRequest =>
            r.discardEntityBytes() // important to drain incoming HTTP Entity stream
            HttpResponse(404, entity = "Unknown resource!")
        })
      }).run()
  }
}
