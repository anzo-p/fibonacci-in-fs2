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
      value.round,
      Some(toProtobuf(value.lowInteger)),
      Some(toProtobuf(value.highInteger))
    )

  def fromByteArray(value: Option[Array[Byte]]): Either[ConversionError, FibonacciProto] = {
    val data = value.getOrElse("".getBytes)

    FibonacciProto
      .validate(data)
      .toEither
      .leftMap(_ => ConversionError(s"cannot extract FibonacciProto from record value: $data"))
  }

  def fromProtobuf(proto: BigIntegerProto): BigInt =
    BigInt(
      proto.value.toByteArray
    )

  def fromProtobuf(proto: FibonacciProto): Either[DecodeError, Fibonacci] = {
    val validated: Option[Either[ValidationError, Fibonacci]] = for {
      lo <- proto.lowInteger
      hi <- proto.highInteger
    } yield Fibonacci.create(
      proto.round,
      fromProtobuf(lo),
      fromProtobuf(hi)
    )

    validated match {
      case None =>
        Left(ValidationError(s"cannot validate Fibonacci from input: ${proto.toProtoString}"))

      case Some(a) =>
        a
    }
  }
}
