package sttp.tapir.server.netty.loom

import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.internal.{NettyServerInterpreter, RunAsync}

trait NettyIdServerInterpreter {
  def nettyServerOptions: NettyIdServerOptions[_]

  def toRoute(
      ses: List[ServerEndpoint[Any, Id]]
  ): IdRoute = {
    NettyServerInterpreter.toRoute[Id](
      ses,
      nettyServerOptions.interceptors,
      nettyServerOptions.createFile,
      nettyServerOptions.deleteFile,
      new RunAsync[Id] {
        override def apply[T](f: => Id[T]): Unit = {
          f
          ()
        }
      }
    )
  }
}

object NettyIdServerInterpreter {
  def apply(serverOptions: NettyIdServerOptions[_] = NettyIdServerOptions.default): NettyIdServerInterpreter = {
    new NettyIdServerInterpreter {
      override def nettyServerOptions: NettyIdServerOptions[_] = serverOptions
    }
  }
}
