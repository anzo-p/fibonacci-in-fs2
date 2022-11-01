package app

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import fs2.kafka._
import org.typelevel.log4cats.Logger

import java.util.UUID
import scala.concurrent.duration.{DurationInt, FiniteDuration, SECONDS}

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
    - handle errors
    - logging
    - can we ask to stop, start, reset, or request the latest?

    Test
    - everything
   */

  def serialize(value: Fibonacci): Array[Byte] =
    ProtobufConversions.toProtobuf(value).toByteArray

  def deserialize(record: Option[Array[Byte]]): Either[DecodeError, Fibonacci] =
    for {
      proto <- ProtobufConversions.fromByteArray(record)
      value <- ProtobufConversions.fromProtobuf(proto)
    } yield value

  def inc(fibonacci: Fibonacci): Fibonacci =
    if (fibonacci.lowInteger > 1000) {
      Fibonacci(fibonacci.highInteger, fibonacci.highInteger)
    }
    else {
      Fibonacci(fibonacci.highInteger, fibonacci.lowInteger + fibonacci.highInteger)
    }

  def compose(value: Fibonacci): ProducerRecord[Some[String], Array[Byte]] =
    ProducerRecord(topic, Some(UUID.randomUUID().toString), serialize(value))

  def send(record: ProducerRecord[Some[String], Array[Byte]]): F[Either[Throwable, Unit]] =
    Logger[F].debug(s"") *>
      producer.produce(ProducerRecords(record.some)).flatten.void.attempt

  def processEvent(record: ConsumerRecord[Option[String], Option[Array[Byte]]]): F[Unit] = {
    val result: F[Either[Throwable, Unit]] =
      (for {
        currVal <- EitherT.fromEither[F](deserialize(record.value).leftMap(e => new Throwable(e.message)))
        _       <- EitherT.pure[F, Throwable](println(s"currVal $currVal")) // log debug
        nextVal <- EitherT.pure[F, Throwable](inc(currVal))
        _       <- EitherT.right[Throwable](send(compose(nextVal)))
      } yield ()).value

    Thread.sleep(1000)

    result.flatMap {
      case Left(ex) =>
        println(s"[processEvent] resulted in Left: ${ex.getMessage}")
        ex.raiseError[F, Throwable]
          .flatMap(_ => Logger[F].error(ex)(s"Error processing message: $ex")) // the exception should have it all, add only minimal additional logging here

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

    println(s"[sendToDLQ] DLQMessage: $dlqMessage")

    val dlqTopic = s"DLQ-.${committable.record.topic}"
    //val newHeaders = constructDlqMessageHeaders(committable)
    val record = ProducerRecord(dlqTopic, committable.record.key, dlqMessage.getBytes) //.withHeaders(newHeaders)

    for {
      _ <- Logger[F].error(throwable)(dlqMessage) // this is again unnecessary logging
      _ <- producer.produce(ProducerRecords.one(record, committable.offset)).flatten
    } yield {
      Either.left(committable.offset)
    }
  }

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
              println(s"[upStream] throwable: ${throwable.getMessage}")

              val res: F[Either[Throwable, Unit]] = for {
                _ <- Logger[F].error(s"error occurred: $throwable") // its already logged ad processEvent, not sure if needs logging here
              } yield Either.left[Throwable, Unit](throwable)

              // tick failure
              res.flatMap(_ => sendToDLQ(committable, throwable))

            case Right(_) =>
              // tick success
              EitherT.rightT[F, CommittableOffset[F]](committable.offset).value
          }
          .as(committable.offset)
      }
      .through(commitBatchWithin(500, 15.seconds))
      //.handleErrorWith { throwable => tick fatal failure; pass to sentry }
      .attempts(fs2.Stream.constant(FiniteDuration(10, SECONDS)))
      .void
  }
}
/*
  how do we make errors?
  - processEvent fails either on ProtobufConversions.fromByteArray or *fromProtobuf
  - then again arent we bringing only Either String, Value? we arent throwing anything..
  - how do we catch a fail in processEvent unless we throw?

  courierpayments-intake/.../test/.../payment/PaymentBatchJobStreamSpec
  - io.assertThrows[Throwable]
  - these tests assert that the processing of event throws exceptions in given situations

  courierpayments-intake/.../it/.../ingest/CourierStreamIT and PaymentTransactionStreamIT
  - for { _ <- processRecord .. yield .. }.asserting(..)

  CDCStreamBase override on processKafkaRecord returns Either[Error, Success]
  PaymentBatchJobStream, PurchaseStream .. returns Either[Exception, Success]

  They all return Either[Throwable, Success]
  But CDCStreamBase overrides contain their own error handler
  Until it gets to top level where the error is passed to Sentry and then written to DLQ

  most processors are so simple that once decode successful they assume the rest cannot fail
  - successful internal model stored to db

  as soon as a procerssor may fail, the failure is catched, sentried, ticked for failure
  and then reraised inside Either Left, for the main handler to  send to DLQ

  also they mostly seem to catch decode errors
  once the data is in internal model, they seem to assume success path

  not yet immediately obvious that commitBatch and sendToDLQ are tested
 */
