package app.kafka.serdes

import app.models.Fibonacci
import cats.implicits._
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

  def fromByteArray(value: Option[Array[Byte]]): Either[ConversionError, FibonacciProto] =
    FibonacciProto
      .validate(value.getOrElse("".getBytes))
      .toEither
      .leftMap(_ => ConversionError(s"cannot extract FibonacciProto from record value: $value"))

  def fromProtobuf(proto: BigIntegerProto): BigInt =
    BigInt(
      proto.value.toByteArray
    )

  def fromProtobuf(proto: FibonacciProto): Either[DecodeError, Fibonacci] = {
    val validated: Option[Either[ValidationError, Fibonacci]] = for {
      lo <- proto.lowInteger
      hi <- proto.highInteger
    } yield Fibonacci.create(
      fromProtobuf(lo),
      fromProtobuf(hi)
    )

    validated match {
      case None =>
        Left(ConversionError(s"cannot extract Fibonacci from input: ${proto.toProtoString}"))

      case Some(a) =>
        a
    }
  }
}
