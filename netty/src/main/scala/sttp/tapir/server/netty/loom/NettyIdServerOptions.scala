package sttp.tapir.server.netty.loom

import com.typesafe.scalalogging.Logger
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.log.{DefaultServerLog, ServerLog}
import sttp.tapir.server.interceptor.{CustomiseInterceptors, Interceptor}
import sttp.tapir.server.netty.{NettyDefaults, NettyOptions}
import sttp.tapir.{Defaults, TapirFile}

import java.net.{InetSocketAddress, SocketAddress}

case class NettyIdServerOptions[SA <: SocketAddress](
    interceptors: List[Interceptor[Id]],
    createFile: ServerRequest => TapirFile,
    deleteFile: TapirFile => Unit,
    nettyOptions: NettyOptions[SA]
) {
  def prependInterceptor(i: Interceptor[Id]): NettyIdServerOptions[SA] = copy(interceptors = i :: interceptors)
  def appendInterceptor(i: Interceptor[Id]): NettyIdServerOptions[SA] = copy(interceptors = interceptors :+ i)
  def nettyOptions[SA2 <: SocketAddress](o: NettyOptions[SA2]): NettyIdServerOptions[SA2] = copy(nettyOptions = o)
}

object NettyIdServerOptions {

  /** Default options, using TCP sockets (the most common case). This can be later customised using [[NettyIdServerOptions#nettyOptions()]].
    */
  def default: NettyIdServerOptions[InetSocketAddress] = customiseInterceptors.options

  private def default[SA <: SocketAddress](
      interceptors: List[Interceptor[Id]],
      nettyOptions: NettyOptions[SA]
  ): NettyIdServerOptions[SA] =
    NettyIdServerOptions(
      interceptors,
      _ => Defaults.createTempFile(),
      Defaults.deleteFile(),
      nettyOptions
    )

  /** Customise the interceptors that are being used when exposing endpoints as a server. By default uses TCP sockets (the most common
    * case), but this can be later customised using [[NettyIdServerOptions#nettyOptions()]].
    */
  def customiseInterceptors: CustomiseInterceptors[Id, NettyIdServerOptions[InetSocketAddress]] = {
    CustomiseInterceptors(
      createOptions =
        (ci: CustomiseInterceptors[Id, NettyIdServerOptions[InetSocketAddress]]) => default(ci.interceptors, NettyOptions.default)
    ).serverLog(defaultServerLog)
  }

  private val log = Logger[NettyIdServerInterpreter]

  lazy val defaultServerLog: ServerLog[Id] = {
    DefaultServerLog[Id](
      doLogWhenReceived = debugLog(_, None),
      doLogWhenHandled = debugLog,
      doLogAllDecodeFailures = debugLog,
      doLogExceptions = (msg: String, ex: Throwable) => log.error(msg, ex),
      noLog = ()
    )
  }

  private def debugLog(msg: String, exOpt: Option[Throwable]): Unit = NettyDefaults.debugLog(log, msg, exOpt)
}
