package app.kafka

import app.config.{AppConfig, KafkaClientConfig}
import cats.effect.Sync
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, ProducerSettings}

trait KafkaClientSettings {

  def props(config: KafkaClientConfig): Map[String, String] =
    Map(
      )

  def consumerSettings[F[_] : Sync](config: AppConfig): ConsumerSettings[F, Option[String], Option[Array[Byte]]] =
    ConsumerSettings[F, Option[String], Option[Array[Byte]]]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(config.kafkaClientConfigs.bootstrapServers)
      .withGroupId(config.kafkaClientConfigs.consumerGroupId)
      .withProperties(props(config.kafkaClientConfigs))

  def producerSettings[F[_] : Sync](config: AppConfig): ProducerSettings[F, Option[String], Array[Byte]] =
    ProducerSettings[F, Option[String], Array[Byte]]
      .withBootstrapServers(config.kafkaClientConfigs.bootstrapServers)
      .withProperties(props(config.kafkaClientConfigs))
}
