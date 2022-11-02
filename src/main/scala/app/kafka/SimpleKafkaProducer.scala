package app.kafka

import app.streams.FibonacciStreamBase
import cats.effect.Async
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings}

class SimpleKafkaProducer[F[_] : Async](producerSettingsMain: ProducerSettings[F, Option[String], Array[Byte]]) extends FibonacciStreamBase {
  import cats.implicits._

  def send(record: ProducerRecord[Option[String], Array[Byte]]): F[Either[Throwable, Unit]] = {
    KafkaProducer
      .stream(producerSettingsMain)
      .evalMap { producer =>
        producer.produce(ProducerRecords(record.some)).flatten
      }
      .compile
      .drain
      .attempt
  }
}
