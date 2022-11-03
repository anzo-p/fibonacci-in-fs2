package app.http

import app.kafka.SimpleKafkaProducer
import app.models.Fibonacci
import app.streams.FibonacciStreamBase
import cats.Monad
import cats.data.EitherT
import cats.implicits._
import fs2.kafka.ProducerRecord
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

final class SeedEmitter[F[_] : Logger : Monad](producer: SimpleKafkaProducer[F]) extends FibonacciStreamBase {

  private val dsl = Http4sDsl[F]
  import dsl._

  private def errorResponse(throwable: Throwable): F[Response[F]] = {
    val message = s"failed to produce seed due to error: ${throwable.getMessage}"
    Logger[F].error(s"[SeedEmitter] $message") *>
      InternalServerError(message)
  }

  def emit: F[Response[F]] = {
    val seedOr: F[Either[Throwable, Fibonacci]] =
      (for {
        seed <- EitherT.fromEither[F](Fibonacci.create(0, 1)).leftMap(e => new Throwable(e.message))
      } yield {
        seed
      }).value

    seedOr.flatMap {
      case Left(throwable: Throwable) =>
        errorResponse(throwable)

      case Right(message: Fibonacci) =>
        val record: ProducerRecord[Option[String], Array[Byte]] = producer.compose(serialize(message), topic)

        producer.send(record).flatMap {
          case Left(throwable) =>
            errorResponse(throwable)

          case Right(_) =>
            Created("")
        }
    }
  }
}