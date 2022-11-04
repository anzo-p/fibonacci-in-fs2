package app.streams

import cats.data.EitherT
import cats.effect.Async
import cats.implicits._
import com.redis._
import fs2.kafka.{ConsumerRecord, ConsumerSettings, KafkaProducer, ProducerRecords}
import org.typelevel.log4cats.Logger

final case class FibonacciStream[F[_] : Async : Logger](
    settings: ConsumerSettings[F, Option[String], Option[Array[Byte]]],
    producer: KafkaProducer[F, Option[String], Array[Byte]],
    redis: RedisClient
  ) extends UpStream[F](settings, producer)
    with FibonacciStreamBase {

  override protected def processEvent(record: ConsumerRecord[Option[String], Option[Array[Byte]]]): F[Unit] = {
    val result: F[Either[Throwable, Unit]] =
      (for {
        currVal   <- EitherT.fromEither[F](deserialize(record.value).leftMap(e => new Throwable(e.message)))
        nextVal   <- EitherT.pure[F, Throwable](currVal.inc)
        nextEvent <- EitherT.pure[F, Throwable](compose(topic, serialize(nextVal)))
        _         <- EitherT.right[Throwable](send(ProducerRecords.one(nextEvent)))
        _         <- EitherT.pure[F, Throwable](redis.set("latest.fibo", nextVal.toJson))
      } yield ()).value

    Thread.sleep(500)

    result.flatMap {
      case Left(th) =>
        th.raiseError[F, Throwable]
          .flatMap(_ => Logger[F].error(th)(s"Error processing message: $th"))

      case Right(_) =>
        ().pure[F]
    }
  }
}
