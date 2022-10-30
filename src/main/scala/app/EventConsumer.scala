package app

import cats.effect._
import cats.Parallel
import fs2.Stream
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaProducer, ProducerSettings}
import org.typelevel.log4cats.Logger

final class EventConsumer[F[_] : Async : Parallel : Logger] {

  private val producerSettings = ProducerSettings[F, Option[String], Array[Byte]]
    .withBootstrapServers("localhost:9093")
    .withProperties(Map[String, String]())

  private val consumerSettings = ConsumerSettings[F, Option[String], Option[Array[Byte]]]
    .withAutoOffsetReset(AutoOffsetReset.Earliest)
    .withBootstrapServers("localhost:9093")
    .withGroupId("consumer.groupId")
    .withProperties(Map[String, String]())

  val streams: Stream[F, Unit] = KafkaProducer
    .stream(producerSettings)
    .flatMap { producer =>
      val streams = List(
        new UpStream[F](consumerSettings, producer).upStream
      )
      Stream(streams: _*).parJoinUnbounded
    }
}

object EventConsumer {

  def make[F[_] : Async : Parallel : Logger]: F[EventConsumer[F]] =
    Sync[F].delay(new EventConsumer[F])
}
