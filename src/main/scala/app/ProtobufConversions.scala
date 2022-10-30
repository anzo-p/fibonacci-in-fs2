package app

import app.Fibonacci.ValidationResult
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.toBifunctorOps
import com.anzop.fibonacciProtocol.{BigIntegerProto, FibonacciProto}
import com.google.protobuf.ByteString

object ProtobufConversions {

  def toProtobuf(k: BigInt): BigIntegerProto =
    new BigIntegerProto(
      ByteString.copyFrom(k.toByteArray)
    )

  def toProtobuf(value: Fibonacci): FibonacciProto =
    FibonacciProto(
      Some(toProtobuf(value.lowInteger)),
      Some(toProtobuf(value.highInteger))
    )

  def fromByteArray(value: Option[Array[Byte]]): Either[String, FibonacciProto] =
    FibonacciProto
      .validate(value.getOrElse("".getBytes))
      .toEither
      .leftMap(_ => s"cannot extract FibonacciProto from record value: $value")

  def fromProtobuf(proto: BigIntegerProto): BigInt =
    BigInt(
      proto.value.toByteArray
    )

  def fromProtobuf(proto: FibonacciProto): Either[String, Fibonacci] = {
    val validated: Option[ValidationResult[Fibonacci]] = for {
      lo <- proto.lowInteger
      hi <- proto.highInteger
    } yield Fibonacci
      .validateFibonacci(
        fromProtobuf(lo),
        fromProtobuf(hi)
      )

    validated match {
      case None =>
        Left("")

      case Some(Invalid(e)) =>
        Left(e.toString)

      case Some(Valid(a)) =>
        Right(a)
    }
  }
}
