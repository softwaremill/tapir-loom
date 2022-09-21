package sttp.tapir.server.nima

import cats.effect.{IO, Resource}
import org.scalatest.EitherValues
import sttp.monad.FutureMonad
import sttp.tapir.server.nima.internal.IdMonad
import sttp.tapir.server.tests.*
import sttp.tapir.tests.{Test, TestSuite}

class NimaServerTest extends TestSuite with EitherValues {
  override def tests: Resource[IO, List[Test]] =
    backendResource.flatMap { backend =>
      Resource
        .eval(IO.delay {
          val interpreter = new NimaTestServerInterpreter()
          val createServerTest = new DefaultCreateServerTest(backend, interpreter)

          new ServerBasicTests(createServerTest, interpreter, invulnerableToUnsanitizedHeaders = false).tests() ++
            new AllServerTests(createServerTest, interpreter, backend, basic = false, multipart = false).tests()
        })
    }
}
