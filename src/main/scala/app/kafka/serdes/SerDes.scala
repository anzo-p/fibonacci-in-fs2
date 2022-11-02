package app.kafka.serdes

trait SerDes[A] {

  def serialize(a: A): Array[Byte]

  def deserialize(record: Option[Array[Byte]]): Either[DecodeError, A]
}
