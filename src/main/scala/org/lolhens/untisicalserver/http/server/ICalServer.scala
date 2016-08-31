package org.lolhens.untisicalserver.http.server

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.lolhens.untisicalserver.ical.ICalProvider

import scala.concurrent.Future

/**
  * Created by pierr on 31.08.2016.
  */
class ICalServer(val iCalProvider: ICalProvider) {
  def start() = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().bind(interface = "localhost", port = 8080)

    val bindingFuture: Future[Http.ServerBinding] =
      serverSource.to(Sink.foreach { connection =>
        connection.handleWith(Flow[HttpRequest].collect {
          case HttpRequest(GET, Uri.Path("/ical/fs15b.ics"), _, _, _) =>
            HttpResponse(entity = HttpEntity(ContentTypes.`application/octet-stream`, iCalProvider().toString.getBytes(StandardCharsets.UTF_8)))

          case r: HttpRequest =>
            r.discardEntityBytes() // important to drain incoming HTTP Entity stream
            HttpResponse(404, entity = "Unknown resource!")
        })
      }).run()
  }
}
