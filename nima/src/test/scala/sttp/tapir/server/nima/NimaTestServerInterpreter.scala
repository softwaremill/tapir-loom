package sttp.tapir.server.nima

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import io.helidon.nima.webserver.WebServer
import io.helidon.nima.webserver.http.{Handler, HttpRouting}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.tests.TestServerInterpreter
import sttp.tapir.server.nima.{Id, NimaServerInterpreter, NimaServerOptions}
import sttp.tapir.tests.Port

import java.net.InetSocketAddress

class NimaTestServerInterpreter() extends TestServerInterpreter[Id, Any, NimaServerOptions, Handler]:

  override def route(es: List[ServerEndpoint[Any, Id]], interceptors: Interceptors): Handler =
    val serverOptions: NimaServerOptions = interceptors(NimaServerOptions.customiseInterceptors).options
    NimaServerInterpreter(serverOptions).toHandler(es)

  override def server(routes: NonEmptyList[Handler]): Resource[IO, Port] =
    val bind = IO.blocking {
      WebServer
        .builder()
        .routing { (b: HttpRouting.Builder) =>
          routes.iterator.foreach(b.any)
        }
        .start()
    }

    Resource
      .make(bind)(binding => IO.blocking(binding.stop()))
      .map(b => b.port)
