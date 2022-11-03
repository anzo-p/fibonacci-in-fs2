package app.http

import cats.Monad
import com.redis.RedisClient
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class HealthRoutes[F[_] : Monad](redis: RedisClient) {

  private val dsl = Http4sDsl[F]
  import dsl._

  def routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "liveness" =>
        Ok()

      case GET -> Root / "readiness" =>
        redis.get("dummy")
        Ok()
    }
}
