import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.0.0"

  val compile = Seq(
  // format: OFF
    "uk.gov.hmrc"    %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "org.typelevel"  %% "cats-core"                    % "2.12.0"
  // format: ON
  )

  val test = Seq(
  // format: OFF
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"  % bootstrapVersion % "test",
    "com.github.pjfanning" %% "pekko-mock-scheduler"    % "0.6.0"          % "test"
  // format: ON
  )

  // only add additional dependencies here - it test inherit test dependencies above already
  val itDependencies: Seq[ModuleID] = Seq()

}
