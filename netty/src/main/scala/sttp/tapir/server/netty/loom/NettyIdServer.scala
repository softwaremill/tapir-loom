package sttp.tapir.server.netty.loom

import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.model.ServerResponse
import sttp.tapir.server.netty.NettyConfig
import sttp.tapir.server.netty.NettyResponse
import sttp.tapir.server.netty.Route
import sttp.tapir.server.netty.internal.NettyBootstrap
import sttp.tapir.server.netty.internal.NettyServerHandler

import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.{Future => JFuture}
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.control.NonFatal

case class NettyIdServer(routes: Vector[IdRoute], options: NettyIdServerOptions, config: NettyConfig) {
  private val executor = Executors.newVirtualThreadPerTaskExecutor()

  def addEndpoint(se: ServerEndpoint[Any, Id]): NettyIdServer = addEndpoints(List(se))
  def addEndpoint(se: ServerEndpoint[Any, Id], overrideOptions: NettyIdServerOptions): NettyIdServer =
    addEndpoints(List(se), overrideOptions)
  def addEndpoints(ses: List[ServerEndpoint[Any, Id]]): NettyIdServer = addRoute(NettyIdServerInterpreter(options).toRoute(ses))
  def addEndpoints(ses: List[ServerEndpoint[Any, Id]], overrideOptions: NettyIdServerOptions): NettyIdServer =
    addRoute(NettyIdServerInterpreter(overrideOptions).toRoute(ses))

  def addRoute(r: IdRoute): NettyIdServer = copy(routes = routes :+ r)
  def addRoutes(r: Iterable[IdRoute]): NettyIdServer = copy(routes = routes ++ r)

  def options(o: NettyIdServerOptions): NettyIdServer = copy(options = o)
  def config(c: NettyConfig): NettyIdServer = copy(config = c)
  def modifyConfig(f: NettyConfig => NettyConfig): NettyIdServer = config(f(config))

  def host(hostname: String): NettyIdServer = modifyConfig(_.host(hostname))

  def port(p: Int): NettyIdServer = modifyConfig(_.port(p))

  def start(): NettyIdServerBinding =
    startUsingSocketOverride[InetSocketAddress](None) match {
      case (socket, stop) =>
        NettyIdServerBinding(socket, stop)
    }

  def startUsingDomainSocket(path: Option[Path] = None): NettyIdDomainSocketBinding =
    startUsingDomainSocket(path.getOrElse(Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString)))

  def startUsingDomainSocket(path: Path): NettyIdDomainSocketBinding =
    startUsingSocketOverride(Some(new DomainSocketAddress(path.toFile))) match {
      case (socket, stop) =>
        NettyIdDomainSocketBinding(socket, stop)
    }

  private def startUsingSocketOverride[SA <: SocketAddress](socketOverride: Option[SA]): (SA, () => Unit) = {
    val eventLoopGroup = config.eventLoopConfig.initEventLoopGroup()
    val route = Route.combine(routes)

    def unsafeRunF(
        callToExecute: () => Id[ServerResponse[NettyResponse]]
    ): (Future[ServerResponse[NettyResponse]], () => Future[Unit]) = {
      val scalaPromise = Promise[ServerResponse[NettyResponse]]()
      val jFuture: JFuture[?] = executor.submit(new Runnable {
        override def run(): Unit = try {
          val result = callToExecute()
          scalaPromise.success(result)
        }
        catch {
          case NonFatal(e) => scalaPromise.failure(e)
        }
      })
      
      (
        scalaPromise.future,
        () => {
          jFuture.cancel(true)
          Future.unit
        }
      )
    }
    val channelIdFuture = NettyBootstrap(
      config,
      new NettyServerHandler(
        route,
        unsafeRunF,
        config.maxContentLength
      ),
      eventLoopGroup,
      socketOverride
    )
    channelIdFuture.await()
    val channelId = channelIdFuture.channel()

    (
      channelId.localAddress().asInstanceOf[SA],
      () => stop(channelId, eventLoopGroup)
    )
  }

  private def stop(ch: Channel, eventLoopGroup: EventLoopGroup): Unit = {
    ch.close().get()
    if (config.shutdownEventLoopGroupOnClose) {
      val _ = eventLoopGroup.shutdownGracefully().get()
    }
  }
}

object NettyIdServer {
  def apply(): NettyIdServer = NettyIdServer(Vector.empty, NettyIdServerOptions.default, NettyConfig.defaultNoStreaming)

  def apply(serverOptions: NettyIdServerOptions): NettyIdServer =
    NettyIdServer(Vector.empty, serverOptions, NettyConfig.defaultNoStreaming)

  def apply(config: NettyConfig): NettyIdServer =
    NettyIdServer(Vector.empty, NettyIdServerOptions.default, config)

  def apply(serverOptions: NettyIdServerOptions, config: NettyConfig): NettyIdServer =
    NettyIdServer(Vector.empty, serverOptions, config)
}
case class NettyIdServerBinding(localSocket: InetSocketAddress, stop: () => Unit) {
  def hostName: String = localSocket.getHostName
  def port: Int = localSocket.getPort
}
case class NettyIdDomainSocketBinding(localSocket: DomainSocketAddress, stop: () => Unit) {
  def path: String = localSocket.path()
}
