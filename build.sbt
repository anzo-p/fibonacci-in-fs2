ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "fibonacci"
  )

val catsEffect       = "3.2.0"
val circe            = "0.14.2"
val fs2Kafka         = "2.3.+"
val http4s           = "0.23.12"
val log4cats         = "2.3.1"
val pureConfig       = "0.17.1"
val scalaTestVersion = "3.2.10"
val slf4j            = "2.0.1"
val typesafeConfig   = "1.4.2"

libraryDependencies ++= Seq(
  "org.typelevel"         %% "cats-effect"            % catsEffect,
  "com.github.fd4s"       %% "fs2-kafka"              % fs2Kafka,
  "org.http4s"            %% "http4s-dsl"             % http4s,
  "org.http4s"            %% "http4s-blaze-server"    % http4s,
  "io.circe"              %% "circe-generic"          % circe,
  "org.typelevel"         %% "log4cats-slf4j"         % log4cats,
  "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfig,
  "com.github.pureconfig" %% "pureconfig"             % pureConfig,
  "net.debasishg"         %% "redisclient"            % "3.41",
  "com.thesamet.scalapb"  %% "scalapb-runtime"        % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

libraryDependencies ++= Seq(
  "com.typesafe" % "config"       % typesafeConfig,
  "org.slf4j"    % "slf4j-api"    % slf4j,
  "org.slf4j"    % "slf4j-simple" % slf4j
)

libraryDependencies ++= Seq(
  "org.scalatest"     %% "scalatest"                     % scalaTestVersion % Test,
  "org.scalamock"     %% "scalamock"                     % "5.2.0"          % Test,
  "org.scalatestplus" %% "scalacheck-1-15"               % "3.2.11.0"       % Test,
  "org.typelevel"     %% "cats-effect-testing-scalatest" % "1.4.0"          % Test
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)
