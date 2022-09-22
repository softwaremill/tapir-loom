# tapir-loom

[tapir](https://tapir.softwaremill.com/en/latest/) + [loom](https://openjdk.org/projects/loom/) integration.

Requires Java 19, and the `--enable-preview` java option to be provided.

There are currently two server interpreters available:
* one using [Netty](https://netty.io), running the server logic on virtual threads
* and one using an alpha release of [Helidon Nima 4 ](https://medium.com/helidon/helidon-n%C3%ADma-helidon-on-virtual-threads-130bb2ea2088)

Try running [SleepDemo](https://github.com/softwaremill/tapir-loom/blob/master/nima/src/test/scala/sttp/tapir/server/nima/SleepDemo.scala) with some load: `wrk -c 100 http://localhost:8080/hello`.

To use, add one of the following dependencies:

```scala
"com.softwaremill.sttp.tapir" %% "tapir-netty-server-id" % "0.1.1"
// or
"com.softwaremill.sttp.tapir" %% "tapir-nima-server" % "0.1.1"
```

Then, use `NimaServerInterpreter` or the `NettyIdServer` as other [netty server](https://tapir.softwaremill.com/en/latest/server/netty.html) variants.
For example:

```scala
import sttp.tapir._

object SleepDemo extends App {
  val e = endpoint.get.in("hello").out(stringBody).serverLogicSuccess[Id] { _ =>
    Thread.sleep(1000)
    "Hello, world!"
  }
  NettyIdServer().addEndpoint(e).start()
}
```