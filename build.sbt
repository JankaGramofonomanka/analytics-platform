import Dependencies._

lazy val commonSettings = Seq(
  organization := "io.github.JankaGramofonomanka",
  name := "analytics-platform",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.13.8",

  scalacOptions ++= Seq(
    "-Ymacro-annotations",
  ),

  libraryDependencies ++= Seq(

    Libs.http4sEmberServer,
    Libs.http4sEmberClient,
    Libs.http4sCirce,
    Libs.http4sDSL,
    
    Libs.circeGeneric,
    Libs.circeLiteral,
    Libs.circeGenericExtras,
    Libs.circeParser,

    Libs.aerospikeClient,

    Libs.kafkaClient,

    Libs.munit,
    Libs.munitCatsEffect3,
    Libs.logback,
    Libs.svmSubs,
    Libs.apacheCommons,
  ),

  addCompilerPlugin(Libs.kindProjector),
  addCompilerPlugin(Libs.betterMonadicFor),
  testFrameworks += new TestFramework("munit.Framework")
)



lazy val root = (project in file(".")).aggregate(
  frontend,
  aggregateProcessor, 
  common,
)

lazy val frontend = (project in file("frontend"))
  .settings(commonSettings)
  .settings(
    name := "frontend", 
  )
  .dependsOn(common)

lazy val aggregateProcessor = (project in file("aggregate-processor"))
  .settings(commonSettings)
  .settings(
    name := "aggregate-processor",
  )
  .dependsOn(common)

lazy val common = (project in file("common"))
  .settings(commonSettings)
  .settings(
    name := "common",
    
  )

