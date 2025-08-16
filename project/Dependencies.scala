import sbt._

/** Created by Ludovic Temgoua Abanda (icemc) on 19/09/2022 */
object Dependencies {

  private object Versions {
    val zio                     = "2.1.15"
    val zioConfig               = "4.0.3"
    val zioLogging              = "2.4.0"
    val zioLog4j                = "2.4.0"
    val circe                   = "0.14.10"
    val zioInteropCats          = "23.0.0.0"
    val zioResilience           = "0.10.3"
    val ZioHttp                 = "2.0.0-RC11"
    val zioJson                 = "0.3.0-RC10"
    val ZioPrelude              = "1.0.0-RC27"
    val reactivemongo           = "1.1.0-RC6"
    val sttpAuth                = "0.18.0"
    val pbkdf2                  = "0.7.2"
    val bcrypt                  = "4.3.0"
    val jwtCirce                = "10.0.4"
    val organizeImportsVersion  = "0.6.0"
    val tapir                   = "1.11.14"
    val betterMonadicForVersion = "0.3.1"
    val semanticDBVersion       = "4.12.7"
    val kindProjectorVersion    = "0.13.3"
    val mongo4Cats              = "0.7.11"
    val zioSchemaProtobuf       = "1.6.1"
    val zioRedis                = "1.0.0"
    val logback                 = "1.5.16"
    val zioSttpClient           = "3.10.3"
    val nimbusJose              = "10.0.1"
    val logbackEncoderV         = "8.0"
    val zioMetrics              = "2.3.1"
    val lokiAppender            = "1.6.0"
    val zioCache                = "0.2.3"
    val jsonSchemaCirce         = "0.11.7"
    val scalatest               = "3.2.19"
    val apacheCommons           = "1.17.0"
    val googlePhoneNumber       = "8.13.42"
    val apacheCommonValidator   = "1.8.0"
    val resend                  = "3.1.0"
    val twilio                  = "10.6.8"
  }

  object Libraries {

    import Versions._

    val zio = Seq(
      "dev.zio"   %% "zio"          % Versions.zio,
      "dev.zio"   %% "zio-streams"  % Versions.zio,
      "dev.zio"   %% "zio-macros"   % Versions.zio,
      "nl.vroste" %% "rezilience"   % zioResilience,
      "dev.zio"   %% "zio-cache"    % zioCache,
      "dev.zio"   %% "zio-test"     % Versions.zio % Test,
      "dev.zio"   %% "zio-test-sbt" % Versions.zio % Test
    )

    val http = Seq(
      "io.d11" %% "zhttp" % ZioHttp
    )

    val tests = Seq(
      "dev.zio"       %% "zio-test"          % Versions.zio % Test,
      "dev.zio"       %% "zio-test-sbt"      % Versions.zio % Test,
      "dev.zio"       %% "zio-test-magnolia" % Versions.zio % Test,
      "org.scalatest" %% "scalatest"         % scalatest    % Test
    )

    val tapir = Seq(
      "com.softwaremill.sttp.tapir"   %% "tapir-zio-http-server"   % Versions.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"  % Versions.tapir,
      "com.softwaremill.sttp.client3" %% "zio"                     % zioSttpClient,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle" % Versions.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-apispec-docs"      % Versions.tapir
    )

    val metrics = Seq(
      "dev.zio" %% "zio-metrics-connectors"            % Versions.zioMetrics, // core library
      "dev.zio" %% "zio-metrics-connectors-prometheus" % Versions.zioMetrics  // Prometheus client
    )

    val zioConfig = Seq(
      "dev.zio" %% "zio-config"          % Versions.zioConfig,
      "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
      "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig
    )

    val logging = Seq(
      "dev.zio"             %% "zio-logging"              % zioLogging,
      "dev.zio"             %% "zio-logging-slf4j"        % zioLog4j,
      "ch.qos.logback"       % "logback-classic"          % logback,
      "net.logstash.logback" % "logstash-logback-encoder" % logbackEncoderV,
      "com.github.loki4j"    % "loki-logback-appender"    % lokiAppender
    )

    val json = Seq(
      "io.circe"                      %% "circe-core"       % circe,
      "io.circe"                      %% "circe-generic"    % circe,
      "io.circe"                      %% "circe-parser"     % circe,
      "io.github.kirill5k"            %% "mongo4cats-circe" % mongo4Cats,
      "com.softwaremill.sttp.apispec" %% "jsonschema-circe" % jsonSchemaCirce,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe" % Versions.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-play"  % Versions.tapir
    )

    val mongodb = Seq(
      "io.github.kirill5k" %% "mongo4cats-zio"          % mongo4Cats,
      "io.github.kirill5k" %% "mongo4cats-circe"        % mongo4Cats,
      "io.github.kirill5k" %% "mongo4cats-zio-embedded" % mongo4Cats % Test
    )

    val redis = Seq(
      "dev.zio" %% "zio-redis"           % zioRedis,
      "dev.zio" %% "zio-schema-protobuf" % zioSchemaProtobuf,
      "dev.zio" %% "zio-redis-embedded"  % zioRedis % Test
    )

    val auth = Seq(
      "com.github.jwt-scala" %% "jwt-circe"       % jwtCirce,
      "com.ocadotechnology"  %% "sttp-oauth2"     % sttpAuth,
      "io.github.nremond"    %% "pbkdf2-scala"    % pbkdf2,
      "com.github.t3hnar"    %% "scala-bcrypt"    % bcrypt,
      "com.nimbusds"          % "nimbus-jose-jwt" % nimbusJose
    )

    val apache = Seq(
      "commons-codec"     % "commons-codec"     % apacheCommons,
      "commons-validator" % "commons-validator" % Versions.apacheCommonValidator
    )

    val googlePhoneNumber = Seq(
      "com.googlecode.libphonenumber" % "libphonenumber" % Versions.googlePhoneNumber
    )

    val mail = Seq(
      "org.apache.commons" % "commons-email" % "1.5",
      "com.resend"         % "resend-java"   % Versions.resend
    )

    val twillo = Seq("com.twilio.sdk" % "twilio" % twilio)

    val compilerPlugins = Seq(
      compilerPlugin(
        "com.olegpy" %% "better-monadic-for" % betterMonadicForVersion
      ),
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % kindProjectorVersion cross CrossVersion.full
      )
    )

    val cats = Seq("dev.zio" %% "zio-interop-cats" % zioInteropCats)
  }
}
