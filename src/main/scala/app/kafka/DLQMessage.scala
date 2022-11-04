package app.kafka

import io.circe.Json

case class DLQMessage(eventKey: String, error: Json, stackTrace: Json)

object DLQMessage {
  import io.circe._
  import io.circe.syntax._

  implicit val encoder: Encoder[DLQMessage] = (message: DLQMessage) =>
    Json.obj(
      ("eventKey", Json.fromString(message.eventKey)),
      ("error", message.error),
      ("stackTrace", message.stackTrace)
    )

  implicit val decoder: Decoder[DLQMessage] = (c: HCursor) =>
    for {
      key   <- c.downField("eventKey").as[String]
      error <- c.downField("error").as[Json]
      trace <- c.downField("stackTrace").as[Json]
    } yield {
      new DLQMessage(key, error, trace)
    }

  private def apply = new DLQMessage(_, _, _)

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
