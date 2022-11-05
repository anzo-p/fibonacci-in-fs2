package app

import app.config.AppConfig
import app.http.{HealthRoutes, SeedRoutes}
import app.kafka.SimpleKafkaProducer
import app.streams.StreamHandler
import cats.Parallel
import cats.effect._
import cats.implicits._
import com.redis.RedisClient
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  def program[F[_] : Async : Parallel : Logger]: F[ExitCode] =
    AppConfig.load[F]().flatMap { appConfig =>
      val redis  = new RedisClient("localhost", 6379)
      val health = new HealthRoutes[F](redis).routes

      for {
        _      <- Logger[F].info(s"Launching FS2 stream")
        events <- StreamHandler.make[F](appConfig, redis)
        seed = new SeedRoutes[F](redis, new SimpleKafkaProducer[F](events.producerSettings(appConfig))).routes
        _ <- Logger[F].info(s"Launching health server")
        _ <- BlazeServerBuilder[F]
              .bindHttp(8080, "0.0.0.0")
              .withHttpApp((seed <+> health).orNotFound)
              .serve
              .concurrently(events.streams)
              .compile
              .drain

      } yield ExitCode.Success
    }

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    program[IO]
  }
}
