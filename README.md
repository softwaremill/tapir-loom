# tapir-loom

[tapir](https://tapir.softwaremill.com/en/latest/) + [loom](https://openjdk.org/projects/loom/) integration.

Requires Java 19, and the `--enable-preview` java option to be provided.

Currently, a server interpreter using [Helidon Nima](https://medium.com/helidon/helidon-n%C3%ADma-helidon-on-virtual-threads-130bb2ea2088) is available (using its Alpha1 release).

Try running [SleepDemo]() with some load: `wrk -c 100 http://localhost:8080/hello`.