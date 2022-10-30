package app.http

import app.{Fibonacci, ProtobufConversions}
import cats.Monad
import cats.effect.Async
import cats.implicits.toSemigroupKOps
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes, Response}

trait SimpleProducer[F[_]] {
  def send(records: List[ProducerRecord[Option[String], Array[Byte]]]): F[Either[Throwable, Unit]]
}

class KafkaSimpleProducer[F[_] : Async](producerSettingsMain: ProducerSettings[F, Option[String], Array[Byte]]) extends SimpleProducer[F] {
  import cats.implicits._

  override def send(records: List[ProducerRecord[Option[String], Array[Byte]]]): F[Either[Throwable, Unit]] = {
    KafkaProducer
      .stream(producerSettingsMain)
      .evalMap { producer =>
        producer.produce(ProducerRecords(records)).flatten
      }
      .compile
      .drain
      .attempt
  }
}

import org.typelevel.log4cats.Logger

class SeedEmitter[F[_] : Logger : Monad](producer: SimpleProducer[F]) {
  import cats.implicits._
  import fs2.kafka.ProducerRecord

  private val dsl = Http4sDsl[F]
  import dsl._

  def emit: F[Response[F]] = {
    val initEvent = ProtobufConversions.toProtobuf(Fibonacci(0, 1))
    val record    = ProducerRecord("quotations", Some("init-record-key"), initEvent.toByteArray)
    //.withHeaders(..)

    producer.send(List(record)).flatMap {
      case Right(_) =>
        Accepted("")
      case Left(throwable) =>
        //Logger[F].error(throwable)(s"failed to send batch job: $batch")
        InternalServerError("failed to init")
    }
  }

}

class Heartbeat[F[_] : Logger : Monad](producer: SimpleProducer[F]) {

  private val dsl = Http4sDsl[F]
  import dsl._

  private val seedEmitter = new SeedEmitter[F](producer)

  def routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "health" =>
        // make dummy fetch to data sources
        Ok()
    }

  def start: HttpRoutes[F] = {
    HttpRoutes.of {
      case GET -> Root / "reset" =>
        seedEmitter.emit
    }
  }

  def allRoutesComplete: HttpApp[F] =
    (routes <+> start).orNotFound
}
