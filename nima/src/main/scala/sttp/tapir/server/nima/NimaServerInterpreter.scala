package sttp.tapir.server.nima

import io.helidon.common.http.Http
import io.helidon.nima.webserver.http.{Handler, ServerRequest, ServerResponse}
import sttp.tapir.capabilities.NoStreams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.RequestResult
import sttp.tapir.server.interceptor.reject.RejectInterceptor
import sttp.tapir.server.interpreter.{BodyListener, FilterServerEndpoints, ServerInterpreter}
import sttp.tapir.server.nima.internal.{idMonad, NimaBodyListener, NimaRequestBody, NimaServerRequest, NimaToResponseBody}

import java.io.InputStream

trait NimaServerInterpreter {
  def nimaServerOptions: NimaServerOptions

  def toHandler(ses: List[ServerEndpoint[Any, Id]]): Handler = {
    val filteredEndpoints = FilterServerEndpoints[Any, Id](ses)
    val requestBody = new NimaRequestBody(nimaServerOptions.createFile)
    val responseBody = new NimaToResponseBody
    val interceptors = RejectInterceptor.disableWhenSingleEndpoint(nimaServerOptions.interceptors, ses)

    (req: ServerRequest, res: ServerResponse) => {
      implicit val bodyListener: BodyListener[Id, InputStream] = new NimaBodyListener(res)

      val serverInterpreter = new ServerInterpreter[Any, Id, InputStream, NoStreams](
        filteredEndpoints,
        requestBody,
        responseBody,
        interceptors,
        nimaServerOptions.deleteFile
      )

      serverInterpreter(NimaServerRequest(req)) match {
        case RequestResult.Response(response) =>
          res.status(Http.Status.create(response.code.code))
          response.headers.groupBy(_.name).foreach { case (name, headers) =>
            res.header(name, headers.map(_.value): _*)
          }
          response.body.foreach { is =>
            val os = res.outputStream()
            is.transferTo(os)
            os.close()
          }

        case RequestResult.Failure(_) =>
          val ignore = res.next()
      }
    }
  }
}

object NimaServerInterpreter {
  def apply(serverOptions: NimaServerOptions = NimaServerOptions.Default): NimaServerInterpreter =
    new NimaServerInterpreter {
      override def nimaServerOptions: NimaServerOptions = serverOptions
    }
}
