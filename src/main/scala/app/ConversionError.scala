package app

trait DecodeError {
  val message: String
}

case class ConversionError(message: String) extends DecodeError

case class ValidationError(message: String) extends DecodeError
