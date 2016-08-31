package org.lolhens.untisicalserver.http.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future

/**
  * Created by pierr on 31.08.2016.
  */
class ICalServer {
def test = {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().bind(interface = "localhost", port = 8080)

  val bindingFuture: Future[Http.ServerBinding] =
    serverSource.to(Sink.foreach { connection =>
      //connection.handleWith()
      // foreach materializes the source
      println("Accepted new connection from " + connection.remoteAddress)
      // ... and then actually handle the connection
    }).run()
}
}
