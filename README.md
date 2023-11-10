# tapir-loom

## Servers moved to Tapir main repo ⚠️
This repository is now archived. Since Tapir 1.9.0, loom-based backends are migrated to the main Tapir repository:
* [tapir-netty-loom](https://tapir.softwaremill.com/en/latest/server/netty.html)
* [tapir-nima-server](https://tapir.softwaremill.com/en/latest/server/nima.html)
---

[tapir](https://tapir.softwaremill.com/en/latest/) + [loom](https://openjdk.org/projects/loom/) integration.

Requires Java 21.

There are currently two server interpreters available:
* one using [Netty](https://netty.io), running the server logic on virtual threads
* and one using [Helidon Nima 4](https://helidon.io/nima)

Try running [SleepDemo](https://github.com/softwaremill/tapir-loom/blob/master/nima/src/test/scala/sttp/tapir/server/nima/SleepDemo.scala) with some load: `wrk -c 100 http://localhost:8080/hello`.

To use, add one of the following dependencies:

```scala<Up>
"com.softwaremill.sttp.tapir" %% "tapir-netty-server-id" % "0.2.5"
// or
"com.softwaremill.sttp.tapir" %% "tapir-nima-server" % "0.2.5"
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

An example can be found in `nima/src/test/scala/sttp/tapir/server/nima/SleepDemo.scala`, which can be started with `nima/Test/run`.

To enable debug logs for tests, adjust `nima/src/test/resources/logback.xml`.
