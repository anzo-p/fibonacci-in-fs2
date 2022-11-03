package app.http

import app.kafka.SimpleKafkaProducer
import cats.Monad
import com.redis.RedisClient
import org.http4s.dsl.Http4sDsl
import org.http4s.{Header, HttpRoutes}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger

final class SeedRoutes[F[_] : Logger : Monad](redis: RedisClient, producer: SimpleKafkaProducer[F]) {

  private val dsl = Http4sDsl[F]
  import dsl._

  private val seedEmitter = new SeedEmitter[F](producer)

  def routes: HttpRoutes[F] =
    HttpRoutes.of {
      case GET -> Root / "reset" =>
        seedEmitter.emit

      case GET -> Root / "latest" =>
        redis.get("latest.fibo") match {
          case None =>
            Ok("no data")

          case Some(fibo) =>
            Ok(fibo, Header.Raw(CIString("Content-type"), "application/json"))
        }
    }
}
