package app

import app.http.{Heartbeat, KafkaSimpleProducer}
import cats.effect._
import cats.implicits._
import cats.Parallel
import fs2.kafka.ProducerSettings
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  def program[F[_] : Async : Parallel : Logger]: F[ExitCode] = {
    val producerSettings = ProducerSettings[F, Option[String], Array[Byte]]
      .withBootstrapServers("localhost:9093")
      .withProperties(Map[String, String]())

    for {
      _      <- Logger[F].info(s"starting up")
      events <- EventConsumer.make[F]
      _ <- BlazeServerBuilder[F]
            .bindHttp(8080, "0.0.0.0")
            .withHttpApp(new Heartbeat(new KafkaSimpleProducer[F](producerSettings)).allRoutesComplete)
            .serve
            .concurrently(events.streams)
            .compile
            .drain

    } yield ExitCode.Success
  }
  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger = Slf4jLogger.getLogger[IO]

    program[IO]
  }
}
