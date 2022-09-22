package sttp.tapir.server.netty.loom

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import io.netty.channel.nio.NioEventLoopGroup
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.NettyOptions
import sttp.tapir.server.tests.TestServerInterpreter
import sttp.tapir.tests.Port

import java.net.InetSocketAddress

class NettyIdTestServerInterpreter(eventLoopGroup: NioEventLoopGroup)
    extends TestServerInterpreter[Id, Any, NettyIdServerOptions[InetSocketAddress], IdRoute] {
  override def route(es: List[ServerEndpoint[Any, Id]], interceptors: Interceptors): IdRoute = {
    val serverOptions: NettyIdServerOptions[InetSocketAddress] = interceptors(NettyIdServerOptions.customiseInterceptors).options
    NettyIdServerInterpreter(serverOptions).toRoute(es)
  }

  override def server(routes: NonEmptyList[IdRoute]): Resource[IO, Port] = {
    val options = NettyIdServerOptions.default.nettyOptions(
      NettyOptions.default.eventLoopGroup(eventLoopGroup).randomPort.noShutdownOnClose
    )
    val bind = IO.blocking(NettyIdServer(options).addRoutes(routes.toList).start())

    Resource
      .make(bind)(binding => IO.blocking(binding.stop()))
      .map(b => b.port)
  }
}
