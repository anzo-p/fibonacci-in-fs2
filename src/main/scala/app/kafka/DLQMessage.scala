package app.kafka

import io.circe.Json

case class DLQMessage(eventKey: String, error: Json, stackTrace: Json)

object DLQMessage {
  import io.circe._
  import io.circe.generic.semiauto._
  import io.circe.syntax._

  implicit val decode: Decoder[DLQMessage] = deriveDecoder
  implicit val encode: Encoder[DLQMessage] = deriveEncoder

  def apply(eventKey: String, throwable: Throwable): DLQMessage =
    DLQMessage(
      eventKey   = eventKey,
      error      = throwable.toString.asJson,
      stackTrace = throwable.getStackTrace.toList.map(_.toString).asJson
    )

  implicit class ExtendDLQMessage(message: DLQMessage) {

    def toJson: String =
      message.asJson.toString
  }
}
