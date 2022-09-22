package sttp.tapir.server.netty

package object loom {
  type Id[X] = X
  type IdRoute = Route[Id]
}
