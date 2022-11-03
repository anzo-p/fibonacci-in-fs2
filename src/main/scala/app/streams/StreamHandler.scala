package app.streams

import app.config.AppConfig
import app.kafka.KafkaClientSettings
import cats.Parallel
import cats.effect._
import com.redis.RedisClient
import fs2.Stream
import fs2.kafka.KafkaProducer
import org.typelevel.log4cats.Logger

final class StreamHandler[F[_] : Async : Parallel : Logger](appConfig: AppConfig, redis: RedisClient) extends KafkaClientSettings {

  val streams: Stream[F, Unit] = KafkaProducer
    .stream(producerSettings(appConfig))
    .flatMap { producer =>
      val streams = List(
        new FibonacciStream[F](consumerSettings(appConfig), producer, redis).stream
      )
      Stream(streams: _*).parJoinUnbounded
    }
}

object StreamHandler {

  def make[F[_] : Async : Parallel : Logger](appConfig: AppConfig, redis: RedisClient): F[StreamHandler[F]] =
    Sync[F].delay(new StreamHandler[F](appConfig, redis))
}
