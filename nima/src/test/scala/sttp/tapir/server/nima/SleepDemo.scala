package sttp.tapir.server.nima

import io.helidon.nima.webserver.WebServer
import io.helidon.nima.webserver.http.HttpRouting
import sttp.tapir.*

object SleepDemo extends App:
  val e = endpoint.get.in("hello").out(stringBody).serverLogicSuccess[Id] { _ =>
    Thread.sleep(1000)
    "hello, world!"
  }
  val h = NimaServerInterpreter().toHandler(List(e))
  WebServer.builder().routing(_.any(h)).port(8080).start()
  println("Started")
