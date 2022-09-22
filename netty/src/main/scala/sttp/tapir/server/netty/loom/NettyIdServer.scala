package sttp.tapir.server.netty.loom

import io.netty.channel.{Channel, EventLoopGroup}
import io.netty.channel.unix.DomainSocketAddress
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.Route
import sttp.tapir.server.netty.internal.{NettyBootstrap, NettyServerHandler}

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.file.Path
import java.util.concurrent.Executors
import scala.concurrent.Future

case class NettyIdServer[SA <: SocketAddress](routes: Vector[IdRoute], options: NettyIdServerOptions[SA]) {
  private val executor = Executors.newVirtualThreadPerTaskExecutor()

  def addEndpoint(se: ServerEndpoint[Any, Id]): NettyIdServer[SA] = addEndpoints(List(se))
  def addEndpoint(se: ServerEndpoint[Any, Id], overrideOptions: NettyIdServerOptions[SA]): NettyIdServer[SA] =
    addEndpoints(List(se), overrideOptions)
  def addEndpoints(ses: List[ServerEndpoint[Any, Id]]): NettyIdServer[SA] = addRoute(NettyIdServerInterpreter(options).toRoute(ses))
  def addEndpoints(ses: List[ServerEndpoint[Any, Id]], overrideOptions: NettyIdServerOptions[SA]): NettyIdServer[SA] =
    addRoute(NettyIdServerInterpreter(overrideOptions).toRoute(ses))

  def addRoute(r: IdRoute): NettyIdServer[SA] = copy(routes = routes :+ r)
  def addRoutes(r: Iterable[IdRoute]): NettyIdServer[SA] = copy(routes = routes ++ r)

  def options[SA2 <: SocketAddress](o: NettyIdServerOptions[SA2]): NettyIdServer[SA2] = copy(options = o)

  def host(hostname: String)(implicit isTCP: SA =:= InetSocketAddress): NettyIdServer[InetSocketAddress] = {
    val nettyOptions = options.nettyOptions.host(hostname)
    options(options.nettyOptions(nettyOptions))
  }

  def port(p: Int)(implicit isTCP: SA =:= InetSocketAddress): NettyIdServer[InetSocketAddress] = {
    val nettyOptions = options.nettyOptions.port(p)
    options(options.nettyOptions(nettyOptions))
  }

  def domainSocketPath(path: Path)(implicit isDomainSocket: SA =:= DomainSocketAddress): NettyIdServer[DomainSocketAddress] = {
    val nettyOptions = options.nettyOptions.domainSocketPath(path)
    options(options.nettyOptions(nettyOptions))
  }

  def start(): NettyIdServerBinding[SA] = {
    val eventLoopGroup = options.nettyOptions.eventLoopConfig.initEventLoopGroup()
    val route = Route.combine(routes)

    val channelIdFuture = NettyBootstrap(
      options.nettyOptions,
      new NettyServerHandler(
        route,
        (f: () => Id[Unit]) => {
          executor.submit(new Runnable {
            override def run(): Unit = f()
          })
          Future.successful(())
        }
      ),
      eventLoopGroup
    )
    channelIdFuture.await()
    val channelId = channelIdFuture.channel()

    NettyIdServerBinding(
      channelId.localAddress().asInstanceOf[SA],
      () => stop(channelId, eventLoopGroup)
    )
  }

  private def stop(ch: Channel, eventLoopGroup: EventLoopGroup): Unit = {
    ch.close().get()
    if (options.nettyOptions.shutdownEventLoopGroupOnClose) {
      val _ = eventLoopGroup.shutdownGracefully().get()
    }
  }
}

object NettyIdServer {
  def apply(): NettyIdServer[InetSocketAddress] = NettyIdServer(Vector.empty, NettyIdServerOptions.default)

  def apply[SA <: SocketAddress](serverOptions: NettyIdServerOptions[SA]): NettyIdServer[SA] =
    NettyIdServer[SA](Vector.empty, serverOptions)
}

case class NettyIdServerBinding[SA <: SocketAddress](localSocket: SA, stop: () => Unit) {
  def hostName(implicit isTCP: SA =:= InetSocketAddress): String = isTCP(localSocket).getHostName
  def port(implicit isTCP: SA =:= InetSocketAddress): Int = isTCP(localSocket).getPort
  def path(implicit isDomainSocket: SA =:= DomainSocketAddress): String = isDomainSocket(localSocket).path()
}
