package app.streams

import app.kafka.serdes.ValidationError
import app.models.{DLQMessage, Fibonacci}
import cats.data.EitherT
import cats.effect.Async
import cats.implicits._
import fs2.kafka._
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.{DurationInt, FiniteDuration, SECONDS}

/*
  StreamHandler: rename val 'streams'. List(streams), UpStream, and .upStream

  are KafkaClientSettings configs or kafka-related?

  is DLQMessage a model or kafka-related?

  we need a way to monitor the fibo progress
 */

final case class UpStream[F[_] : Async : Logger](
    settings: ConsumerSettings[F, Option[String], Option[Array[Byte]]],
    producer: KafkaProducer[F, Option[String], Array[Byte]]
  ) extends FibonacciStreamBase {

  def inc(fibonacci: Fibonacci): Either[ValidationError, Fibonacci] =
    Fibonacci.create(
      fibonacci.highInteger,
      fibonacci.lowInteger + fibonacci.highInteger
    )

  def send[P](records: ProducerRecords[P, Option[String], Array[Byte]]): F[Either[Throwable, Unit]] =
    Logger[F].debug(s"[UpStream - send] publishing records: ${records.records.toString}") *>
      producer.produce(records).flatten.void.attempt

  def processEvent(record: ConsumerRecord[Option[String], Option[Array[Byte]]]): F[Unit] = {
    val result: F[Either[Throwable, Unit]] =
      (for {
        currVal   <- EitherT.fromEither[F](deserialize(record.value).leftMap(e => new Throwable(e.message)))
        _         <- EitherT.pure[F, Throwable](Logger[F].debug(s"[UpStream - processEvent] Fibonacci currently at: ${currVal.highInteger}"))
        nextVal   <- EitherT.fromEither[F](inc(currVal)).leftMap(e => new Throwable(e.message))
        nextEvent <- EitherT.pure[F, Throwable](compose(serialize(nextVal), topic))
        _         <- EitherT.right[Throwable](send(ProducerRecords.one(nextEvent)))
      } yield ()).value

    Thread.sleep(1000)

    result.flatMap {
      case Left(th) =>
        th.raiseError[F, Throwable]
          .flatMap(_ => Logger[F].error(th)(s"Error processing message: $th"))

      case Right(_) =>
        ().pure[F]
    }
  }

  private def sendToDLQ(
      committable: CommittableConsumerRecord[F, Option[String], Option[Array[Byte]]],
      throwable: Throwable
    ): F[Either[CommittableOffset[F], CommittableOffset[F]]] = {
    import io.circe.syntax._

    val dlqMessage = DLQMessage(
      committable.record.key.getOrElse(""),
      throwable
    ).asJson.toString

    for {
      _ <- Logger[F].error(throwable)(dlqMessage)
      _ <- send(ProducerRecords.one(compose(dlqMessage.getBytes, dlqTopic), committable.offset))
    } yield {
      Either.left(committable.offset)
    }
  }

  def errorHandler(th: Throwable): fs2.Stream[F, Unit] =
    fs2
      .Stream
      .eval(Logger[F].error(s"[UpStream - Fatal Error] replace me with a fatal error tick to metrics and push into sentry the error: $th"))

  val upStream: fs2.Stream[F, Unit] = {
    KafkaConsumer
      .stream(settings)
      .subscribeTo(topic)
      .records
      .filter(_.record.value.isDefined)
      .mapAsync(10) { committable =>
        processEvent(committable.record)
          .attempt
          .flatMap {
            case Left(throwable) =>
              val res: F[Either[Throwable, Unit]] = for {
                _ <- Logger[F].error(s"[UpStream - Recoverable Error] also tick recoverable error to metrics: $throwable")
              } yield Either.left[Throwable, Unit](throwable)

              res.flatMap(_ => sendToDLQ(committable, throwable))

            case Right(_) =>
              Logger[F].debug("[UpStream - Success] replace me with a successful consumption tick to metrics") *>
                EitherT.rightT[F, CommittableOffset[F]](committable.offset).value
          }
          .as(committable.offset)
      }
      .through(commitBatchWithin(500, 15.seconds))
      .handleErrorWith(th => errorHandler(th))
      .attempts(fs2.Stream.constant(FiniteDuration(10, SECONDS)))
      .void
  }
}
