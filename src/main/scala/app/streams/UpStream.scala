package app.streams

import app.kafka.DLQMessage
import cats.data.EitherT
import cats.effect.Async
import cats.implicits._
import fs2.kafka._
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.{DurationInt, FiniteDuration, SECONDS}

abstract class UpStream[F[_] : Async : Logger](
    settings: ConsumerSettings[F, Option[String], Option[Array[Byte]]],
    producer: KafkaProducer[F, Option[String], Array[Byte]]
  ) {

  protected val topic: String

  protected val dlqTopic: String

  protected def compose(value: Array[Byte], topic: String, key: String = ""): ProducerRecord[Some[String], Array[Byte]]

  protected def processEvent(record: ConsumerRecord[Option[String], Option[Array[Byte]]]): F[Unit]

  protected def send[P](records: ProducerRecords[P, Option[String], Array[Byte]]): F[Either[Throwable, Unit]] =
    Logger[F].debug(s"[UpStream - send] publishing records: ${records.records.toString}") *>
      producer.produce(records).flatten.void.attempt

  private def sendToDLQ(
      committable: CommittableConsumerRecord[F, Option[String], Option[Array[Byte]]],
      throwable: Throwable
    ): F[Either[CommittableOffset[F], CommittableOffset[F]]] = {
    val dlqMessage = DLQMessage(committable.record.key.getOrElse(""), throwable).toJson

    for {
      _ <- send(ProducerRecords.one(compose(dlqMessage.getBytes, dlqTopic), committable.offset))
    } yield {
      Either.left(committable.offset)
    }
  }

  private def fatalErrorHandler(th: Throwable): fs2.Stream[F, Unit] =
    fs2
      .Stream
      .eval(Logger[F].error(s"[UpStream - Fatal Error] replace me with a fatal error tick to metrics and push into sentry the error: $th"))

  val stream: fs2.Stream[F, Unit] = {
    KafkaConsumer
      .stream(settings)
      .evalTap(_.subscribeTo(topic))
      .records
      .filter(_.record.value.isDefined)
      .mapAsync(10) { committable =>
        processEvent(committable.record)
          .attempt
          .flatMap {
            case Left(throwable) =>
              Logger[F].error(s"[UpStream - Recoverable Error] tick recoverable error to metrics and log failure reason: $throwable") *>
                sendToDLQ(committable, throwable)

            case Right(_) =>
              Logger[F].debug("[UpStream - Success] replace me with a successful consumption tick to metrics") *>
                EitherT.rightT[F, CommittableOffset[F]](committable.offset).value
          }
          .as(committable.offset)
      }
      .through(commitBatchWithin(500, 15.seconds))
      .handleErrorWith(th => fatalErrorHandler(th))
      .attempts(fs2.Stream.constant(FiniteDuration(10, SECONDS)))
      .void
  }
}
