import sbt._

object Dependencies {

  object V {
    
    val http4s      = "0.23.12"
    val circe       = "0.14.2"
    val aerospike   = "5.1.8"
    val kafka       = "2.8.0"

    val scalatest   = "3.2.13"

  }

  object Libs {

    val http4sEmberServer   = "org.http4s"          %% "http4s-ember-server"  % V.http4s
    val http4sEmberClient   = "org.http4s"          %% "http4s-ember-client"  % V.http4s
    val http4sCirce         = "org.http4s"          %% "http4s-circe"         % V.http4s
    val http4sDSL           = "org.http4s"          %% "http4s-dsl"           % V.http4s
      
    val circeGeneric        = "io.circe"            %% "circe-generic"        % V.circe
    val circeLiteral        = "io.circe"            %% "circe-literal"        % V.circe
    val circeGenericExtras  = "io.circe"            %% "circe-generic-extras" % V.circe
    val circeParser         = "io.circe"            %% "circe-parser"         % V.circe

    val aerospikeClient     = "com.aerospike"       % "aerospike-client"      % V.aerospike

    val kafkaClient         = "org.apache.kafka"    % "kafka-clients"         % V.kafka
    
    val svmSubs             = "org.scalameta"       %% "svm-subs"             % "20.2.0"


    // Test
    val scalatest           = "org.scalatest"       %% "scalatest"            % V.scalatest % Test

    val munit               = "org.scalameta"       %% "munit"                % "0.7.29"    % Test
    val munitCatsEffect3    = "org.typelevel"       %% "munit-cats-effect-3"  % "1.0.7"     % Test

    
    // Runtime
    val logback             = "ch.qos.logback"      %  "logback-classic"      % "1.2.10"    % Runtime
    

    // Plugins
    val kindProjector       = "org.typelevel"       %% "kind-projector"       % "0.13.2" cross CrossVersion.full
    val betterMonadicFor    = "com.olegpy"          %% "better-monadic-for"   % "0.3.1"

  }
}
