
ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "3.3.4"

lazy val scalaCompilerOptions = Seq(
    "-Xfatal-warnings",
    "-Wvalue-discard",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:implicitConversions",
    "-language:strictEquality",
    // required in place of silencer plugin
    "-Wconf:msg=unused-imports&src=html/.*:s",
    "-Wconf:src=routes/.*:s"
)

lazy val microservice = Project("date-calculator", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    scalacOptions       ++= scalaCompilerOptions,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    PlayKeys.playDefaultPort := 8762,
    Compile / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task and not fail the build
    scalafmtOnCompile := true
  )
  .settings(WartRemoverSettings.wartRemoverSettings)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings)
  .settings(CodeCoverageSettings.settings)


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(libraryDependencies ++= AppDependencies.itDependencies)
