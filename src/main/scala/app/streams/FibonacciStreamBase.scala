package app.streams

import app.kafka.serdes.{DecodeError, ProtobufConversions, SerDes}
import app.models.Fibonacci
import fs2.kafka.ProducerRecord

import java.util.UUID

trait FibonacciStreamBase extends SerDes[Fibonacci] {

  val topic: String = "fibonacci.runner.topic"

  val dlqTopic: String = s"dlq-$topic"

  def serialize(value: Fibonacci): Array[Byte] =
    ProtobufConversions.toProtobuf(value).toByteArray

  def deserialize(record: Option[Array[Byte]]): Either[DecodeError, Fibonacci] =
    for {
      proto <- ProtobufConversions.fromByteArray(record)
      value <- ProtobufConversions.fromProtobuf(proto)
    } yield value

  def compose(topic: String, value: Array[Byte], key: String = UUID.randomUUID().toString): ProducerRecord[Some[String], Array[Byte]] =
    ProducerRecord(topic, Some(key), value)
}
