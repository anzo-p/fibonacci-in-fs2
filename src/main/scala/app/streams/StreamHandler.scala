package app.streams

import app.config.AppConfig
import app.kafka.KafkaClientSettings
import cats.Parallel
import cats.effect._
import fs2.Stream
import fs2.kafka.KafkaProducer
import org.typelevel.log4cats.Logger

final class StreamHandler[F[_] : Async : Parallel : Logger](appConfig: AppConfig) extends KafkaClientSettings {

  val streams: Stream[F, Unit] = KafkaProducer
    .stream(producerSettings(appConfig))
    .flatMap { producer =>
      val streams = List(
        new UpStream[F](consumerSettings(appConfig), producer).upStream
      )
      Stream(streams: _*).parJoinUnbounded
    }
}

object StreamHandler {

  def make[F[_] : Async : Parallel : Logger](appConfig: AppConfig): F[StreamHandler[F]] =
    Sync[F].delay(new StreamHandler[F](appConfig))
}
