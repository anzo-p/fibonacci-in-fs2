package app.streams

import app.kafka.serdes.{ConversionError, ProtobufConversions, ValidationError}
import app.models.Fibonacci
import app.utils.ArbitraryGenerator.{sample, OneFibonacci}
import app.utils.BaseSpec
import fs2.kafka.ProducerRecord

import java.util.UUID

class FibonacciStreamBaseSpec extends BaseSpec {

  object TestObject extends FibonacciStreamBase

  val testTopic: String    = sample[String]
  val testValue: Fibonacci = sample[OneFibonacci].value

  val serializedWorkingFibonacciProto: Array[Byte] = ProtobufConversions.toProtobuf(testValue).toByteArray

  "serialize" should {
    "generate correspondent protobuf object in bytearray" in {
      TestObject.serialize(testValue) mustBe ProtobufConversions.toProtobuf(testValue).toByteArray
    }
  }

  "deserialize" should {
    "result in Right Fibonacci instance" when {
      "data deserializes and validates to a Fibonacci instance" in {
        TestObject.deserialize(Some(serializedWorkingFibonacciProto)) mustBe Right(testValue)
      }
    }

    "result in Left ConversionError" when {
      "bytearray cannot be validated" in {
        val serializedDummyData = sample[String].getBytes

        TestObject.deserialize(Some(serializedDummyData)) mustBe Left(
          ConversionError(s"cannot extract FibonacciProto from record value: $serializedDummyData"))
      }
    }

    "result in Left ValidationError" when {
      "Fibonacci validation fails on null FibonacciProto instance" in {
        ProtobufConversions.fromByteArray(None).map { nullFibonacciProto =>
          TestObject.deserialize(None) mustBe Left(
            ValidationError(s"cannot validate Fibonacci from input: ${nullFibonacciProto.toProtoString}"))
        }
      }

      "data deserializes to a FibonacciProto instance but that cannot be validated to a Fibonacci instance" in {
        val serializedInvalidFibonacciProto = ProtobufConversions.toProtobuf(new Fibonacci(0, 1, 2)).toByteArray

        TestObject.deserialize(Some(serializedInvalidFibonacciProto)) mustBe Left(
          ValidationError(s"value 0 in field: 'round' cannot be negative"))
      }
    }
  }

  "compose" should {
    "apply default value to key" in {
      TestObject.compose(testTopic, serializedWorkingFibonacciProto).topic mustBe testTopic
      TestObject.compose(testTopic, serializedWorkingFibonacciProto).value mustBe serializedWorkingFibonacciProto

      UUID.fromString(TestObject.compose(testTopic, serializedWorkingFibonacciProto).key.get).isInstanceOf[UUID] mustBe true
    }

    "allow for passing a string key" in {
      val testKey = sample[String]

      TestObject.compose(testTopic, serializedWorkingFibonacciProto, testKey) mustBe ProducerRecord(
        testTopic,
        Some(testKey),
        serializedWorkingFibonacciProto)
    }
  }
}
