package app

import io.circe._

case class DLQMessage(eventKey: String, error: Json, stackTrace: Json)

object DLQMessage {
  import io.circe.generic.semiauto._
  import io.circe.syntax._

  implicit val decodeDlqMessage: Decoder[DLQMessage] = deriveDecoder
  implicit val encodeDlqMessage: Encoder[DLQMessage] = deriveEncoder

  def apply(eventKey: String, throwable: Throwable): DLQMessage =
    DLQMessage(
      eventKey   = eventKey,
      error      = throwable.toString.asJson,
      stackTrace = throwable.getStackTrace.toList.map(_.toString).asJson
    )
}
