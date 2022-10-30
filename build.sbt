ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "fibonacci"
  )

val catsEffect       = "3.2.0"
val fs2Kafka         = "2.3.+"
val http4sV          = "0.23.12"
val log4catsV        = "2.3.1"
val scalaTestVersion = "3.2.10"
val slf4jV           = "1.7.30"

libraryDependencies ++= Seq(
  "org.typelevel"        %% "cats-effect"         % catsEffect,
  "com.github.fd4s"      %% "fs2-kafka"           % fs2Kafka,
  "org.http4s"           %% "http4s-dsl"          % http4sV,
  "org.http4s"           %% "http4s-blaze-server" % http4sV,
  "org.typelevel"        %% "log4cats-slf4j"      % log4catsV,
  "org.slf4j"            % "slf4j-api"            % slf4jV,
  "com.thesamet.scalapb" %% "scalapb-runtime"     % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

libraryDependencies ++= Seq(
  "org.scalatest"     %% "scalatest"       % scalaTestVersion % Test,
  "org.scalamock"     %% "scalamock"       % "5.2.0"          % Test,
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0"       % Test
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)
