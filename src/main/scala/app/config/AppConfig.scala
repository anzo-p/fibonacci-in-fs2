package app.config

import cats.effect.Sync
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

case class KafkaClientConfig(consumerGroupId: String, bootstrapServers: String)

case class AppConfig(kafkaClientConfigs: KafkaClientConfig)

object AppConfig {

  def loadConfigBlock[F[_] : Sync, T : ConfigReader : ClassTag](config: Config, configPath: String): F[T] =
    ConfigSource
      .fromConfig(config.getConfig(configPath))
      .loadF[F, T]()

  def load[F[_] : Sync](conf: Config): F[AppConfig] = {
    def validateAndCreate(kafkaConfigs: KafkaClientConfig): F[AppConfig] = {
      AppConfig(
        kafkaClientConfigs = kafkaConfigs
      ).pure[F]
    }

    for {
      consumerConfig <- loadConfigBlock[F, KafkaClientConfig](conf, "kafka")
      logger         <- Slf4jLogger.create[F]
      _              <- logger.info("Configurations loaded")
      cfg            <- validateAndCreate(consumerConfig)
    } yield {
      cfg
    }
  }

  def load[F[_] : Sync](): F[AppConfig] =
    Sync[F].blocking(ConfigFactory.load).flatMap(c => load(c))
}
