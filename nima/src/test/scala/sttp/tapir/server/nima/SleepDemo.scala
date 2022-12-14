package sttp.tapir.server.nima

import io.helidon.nima.webserver.WebServer
import sttp.tapir._

object SleepDemo extends App {
  val e = endpoint.get.in("hello").out(stringBody).serverLogicSuccess[Id] { _ =>
    Thread.sleep(1000)
    "hello, world!"
  }
  val h = NimaServerInterpreter().toHandler(List(e))
  WebServer.builder().routing(_.any(h)).port(8080).start()
  println("Started")
}
