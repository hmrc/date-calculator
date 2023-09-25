lazy val scalaCompilerOptions = Seq(
    "-Xfatal-warnings",
    "-Xlint:-missing-interpolator,_",
    "-Xlint:adapted-args",
    "-Ywarn-value-discard",
    "-Ywarn-dead-code",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:implicitConversions",
    // required in place of silencer plugin
    "-Wconf:cat=unused-imports&src=html/.*:s",
    "-Wconf:src=routes/.*:s"
)

lazy val microservice = Project("date-calculator", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    majorVersion        := 0,
    scalaVersion        := "2.13.8",
    scalacOptions       ++= scalaCompilerOptions,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
    PlayKeys.playDefaultPort := 8762
  )
  .settings(WartRemoverSettings.wartRemoverSettings)
  .settings(ScalariformSettings.scalariformSettings: _*)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings)
  .configs(IntegrationTest)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings: _*)
