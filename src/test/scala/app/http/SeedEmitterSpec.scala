package app.http

import app.kafka.SimpleKafkaProducer
import app.utils.ArbitraryGenerator.{sample, OneFibonacci}
import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.kafka.{ProducerRecord, ProducerSettings}
import org.http4s.Status.{Created, InternalServerError}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SeedEmitterSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  class MockSimpleKafkaProducer extends SimpleKafkaProducer[IO](ProducerSettings[IO, Option[String], Array[Byte]]) {
    override def send(record: ProducerRecord[Option[String], Array[Byte]]): IO[Either[Throwable, Unit]] = IO {
      Right(())
    }
  }

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val seedEmitter = new SeedEmitter[IO](new MockSimpleKafkaProducer)

  "SeedEmitter.resolveInitial " - {
    "respond Created for a successful seed" in {
      val successfulSeed = Right(sample[OneFibonacci].value)

      seedEmitter
        .resolveInitial(successfulSeed)
        .asserting(_.status shouldBe Created)
    }

    "respond InternalServerError when initial seed failed as Left Throwable" in {
      val failingSeed = Left(new Throwable(""))

      seedEmitter
        .resolveInitial(failingSeed)
        .asserting(_.status shouldBe InternalServerError)
    }
  }
}
