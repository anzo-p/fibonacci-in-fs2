package app.models

import app.kafka.serdes.ValidationError

case class Fibonacci(round: Long, lowInteger: BigInt, highInteger: BigInt) { self =>

  def inc: Either[ValidationError, Fibonacci] =
    Fibonacci.create(
      self.round + 1,
      self.highInteger,
      self.lowInteger + self.highInteger
    )
}

object Fibonacci {
  import cats.data.Validated._
  import io.circe._
  import io.circe.syntax._

  implicit val encoder: Encoder[Fibonacci] = (fibo: Fibonacci) =>
    Json.obj(
      ("round", Json.fromLong(fibo.round)),
      ("low integer", Json.fromBigInt(fibo.lowInteger)),
      ("high integer", Json.fromBigInt(fibo.highInteger))
    )

  implicit val decoder: Decoder[Fibonacci] = (c: HCursor) =>
    for {
      n  <- c.downField("round").as[Long]
      lo <- c.downField("foo").as[BigInt]
      hi <- c.downField("bar").as[BigInt]
    } yield {
      new Fibonacci(n, lo, hi)
    }

  private def apply = new Fibonacci(_, _, _)

  def create(round: Long, lowInteger: BigInt, highInteger: BigInt): Either[ValidationError, Fibonacci] = {
    ValidateFibonacci.validate(round, lowInteger, highInteger) match {
      case Invalid(e) =>
        Left(ValidationError(e.toChain.toList.mkString(",")))

      case Valid(a) =>
        Right(a)
    }
  }

  implicit class ExtendFibonacci(fibo: Fibonacci) {

    def toJson: String =
      fibo.asJson.toString
  }
}
