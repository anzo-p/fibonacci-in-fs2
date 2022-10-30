package app

import cats.effect._
import cats.implicits._
import fs2.kafka.{ConsumerRecord, ConsumerSettings, KafkaConsumer, KafkaProducer, ProducerRecord, ProducerRecords}
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.concurrent.duration.{FiniteDuration, SECONDS}

final case class UpStream[F[_] : Async : Logger](
    settings: ConsumerSettings[F, Option[String], Option[Array[Byte]]],
    producer: KafkaProducer[F, Option[String], Array[Byte]]
  ) {

  protected def topic: String = "quotations"

  /*
    AppConfig
    - configuration for kafka consumers and producers

    Main
    - logging and metrics

    Heartbeat
    - route "reset" mustnt be here
    - trait SimpleProducer, class KafkaSimpleProducer mustnt be here
    - SeedEmitter mustnt be here
    - logging and metrics

    Fibonacci
    - whats the proper place for validation

    ProtobufConversions
    - what is the proper place for fromByteArray?

    UpStream
    - refactor the logic
    - handle errors
    - logging
    - commit Kafka batch
    - can we ask to stop, start, reset, or request the latest?

    Test
    - everything
   */

  def processEvent(record: ConsumerRecord[Option[String], Option[Array[Byte]]]): F[Unit] = {

    val a: Either[String, Fibonacci] = for {
      proto <- ProtobufConversions.fromByteArray(record.value)
      fibo  <- ProtobufConversions.fromProtobuf(proto)
    } yield fibo

    a match {
      case Left(err) =>
        val message = s"[UpStream.processEvent] failed on error: $err"
        println(message)
        Logger[F].info(message)

      case Right(event) =>
        val message = s"[UpStream.processEvent] consumed event on $event"
        println(message)
        Logger[F].info(message)

        println(s"${event.lowInteger} ${event.highInteger} ${event.lowInteger + event.highInteger}")

        val newEvent = ProtobufConversions.toProtobuf(
          Fibonacci(
            event.highInteger,
            event.lowInteger + event.highInteger
          ))

        import cats.implicits._

        Thread.sleep(1000)

        val record = ProducerRecord("quotations", Some(UUID.randomUUID().toString), newEvent.toByteArray).some

        producer.produce(ProducerRecords(record)).flatten.void <*
          Logger[F].debug(s"produced new event")
    }
  }

  val upStream: fs2.Stream[F, Unit] =
    KafkaConsumer
      .stream(settings)
      .evalTap(_.subscribeTo(topic))
      .flatMap(_.stream)
      .filter(_.record.value.isDefined)
      .mapAsync(10) { committable =>
        processEvent(committable.record)
          .attempt
          .flatMap {
            case Left(err) =>
              Logger[F].info(s"error occurred: $err")

            // log, metrics, producer send to result queue
            case Right(a) =>
              Logger[F].info(s"should commit successful event")
          }
      }
      //.through(commitBatch)
      //.handleErrorWith { throwable => ??? }
      .attempts(fs2.Stream.constant(FiniteDuration(10, SECONDS)))
      .void
}
