package sttp.tapir.server.nima

import com.typesafe.scalalogging.Logger
import io.helidon.common.http.Http
import io.helidon.nima.webserver.http.{Handler, ServerRequest => HelidonServerRequest, ServerResponse => HelidonServerResponse}
import sttp.tapir.capabilities.NoStreams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.RequestResult
import sttp.tapir.server.interceptor.reject.RejectInterceptor
import sttp.tapir.server.interpreter.{BodyListener, FilterServerEndpoints, ServerInterpreter}
import sttp.tapir.server.nima.internal.{NimaBodyListener, NimaRequestBody, NimaServerRequest, NimaToResponseBody, idMonad}

import java.io.InputStream

trait NimaServerInterpreter {
  val log = Logger(getClass.getName)
  def nimaServerOptions: NimaServerOptions

  def toHandler(ses: List[ServerEndpoint[Any, Id]]): Handler = {
    val filteredEndpoints = FilterServerEndpoints[Any, Id](ses)
    val requestBody = new NimaRequestBody(nimaServerOptions.createFile)
    val responseBody = new NimaToResponseBody
    val interceptors = RejectInterceptor.disableWhenSingleEndpoint(nimaServerOptions.interceptors, ses)

    (helidonRequest: HelidonServerRequest, helidonResponse: HelidonServerResponse) => {
      implicit val bodyListener: BodyListener[Id, InputStream] = new NimaBodyListener(helidonResponse)

      val serverInterpreter = new ServerInterpreter[Any, Id, InputStream, NoStreams](
        filteredEndpoints,
        requestBody,
        responseBody,
        interceptors,
        nimaServerOptions.deleteFile
      )

      serverInterpreter(NimaServerRequest(helidonRequest)) match {
        case RequestResult.Response(tapirResponse) =>
          log.debug(s"toHandler: RequestResult.Response: $tapirResponse")
          helidonResponse.status(Http.Status.create(tapirResponse.code.code))
          tapirResponse.headers.groupBy(_.name).foreach { case (name, headers) =>
            helidonResponse.header(name, headers.map(_.value): _*)
          }
          log.debug("toHandler: streaming body")

          tapirResponse.body.fold {
            log.debug("toHandler: streaming body - EMPTY")
            helidonResponse.send()
          } { tapirInputStream =>
            val helidonOutputStream = helidonResponse.outputStream()
            tapirInputStream.transferTo(helidonOutputStream)
            log.debug("toHandler: streaming close")
            helidonOutputStream.close()
          }

        case RequestResult.Failure(_) =>
          val ignore = helidonResponse.next()
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
