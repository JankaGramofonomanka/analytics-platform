val Http4sVersion = "0.23.12"
val CirceVersion = "0.14.2"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.10"
val MunitCatsEffectVersion = "1.0.7"
val AerospikeVersion = "5.1.8"

lazy val root = (project in file("."))
  .settings(
    organization := "io.github.JankaGramofonomanka",
    name := "analytics-platform",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.8",
    
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
    ),
    
    libraryDependencies ++= Seq(
      "org.http4s"          %% "http4s-ember-server"  % Http4sVersion,
      "org.http4s"          %% "http4s-ember-client"  % Http4sVersion,
      "org.http4s"          %% "http4s-circe"         % Http4sVersion,
      "org.http4s"          %% "http4s-dsl"           % Http4sVersion,
      
      "io.circe"            %% "circe-generic"        % CirceVersion,
      "io.circe"            %% "circe-literal"        % CirceVersion,
      "io.circe"            %% "circe-generic-extras" % CirceVersion,
      "io.circe"            %% "circe-parser"         % CirceVersion,

      "org.scalameta"       %% "munit"                % MunitVersion            % Test,
      "org.typelevel"       %% "munit-cats-effect-3"  % MunitCatsEffectVersion  % Test,
      "ch.qos.logback"      %  "logback-classic"      % LogbackVersion          % Runtime,
      "org.scalameta"       %% "svm-subs"             % "20.2.0",
      "com.aerospike"       % "aerospike-client"      % AerospikeVersion,
      "org.apache.commons"  % "commons-lang3"         % "3.12.0",
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
