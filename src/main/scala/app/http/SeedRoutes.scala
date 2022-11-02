package app.http

import app.kafka.SimpleKafkaProducer
import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

final class SeedRoutes[F[_] : Logger : Monad](producer: SimpleKafkaProducer[F]) {

  private val dsl = Http4sDsl[F]
  import dsl._

  private val seedEmitter = new SeedEmitter[F](producer)

  def routes: HttpRoutes[F] = {
    HttpRoutes.of {
      case GET -> Root / "reset" =>
        seedEmitter.emit
    }
  }
}
