package sttp.tapir.server.netty.loom

import cats.effect.{IO, Resource}
import io.netty.channel.nio.NioEventLoopGroup
import org.scalatest.EitherValues
import sttp.monad.FutureMonad
import sttp.tapir.server.netty.internal.FutureUtil.nettyFutureToScala
import sttp.tapir.server.tests.*
import sttp.tapir.tests.{Test, TestSuite}

class NettyIdServerTest extends TestSuite with EitherValues {
  override def tests: Resource[IO, List[Test]] =
    backendResource.flatMap { backend =>
      Resource
        .make(IO.delay {
          implicit val m: FutureMonad = new FutureMonad()
          val eventLoopGroup = new NioEventLoopGroup()

          val interpreter = new NettyIdTestServerInterpreter(eventLoopGroup)
          val createServerTest = new DefaultCreateServerTest(backend, interpreter)

          val tests = new AllServerTests(createServerTest, interpreter, backend, staticContent = false, multipart = false).tests()

          (tests, eventLoopGroup)
        }) { case (_, eventLoopGroup) =>
          IO.fromFuture(IO.delay(nettyFutureToScala(eventLoopGroup.shutdownGracefully()))).void
        }
        .map { case (tests, _) => tests }
    }
}
