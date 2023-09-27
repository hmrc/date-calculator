import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.22.0"

  val compile = Seq(
  // format: OFF
    "uk.gov.hmrc"    %% "bootstrap-backend-play-28"    % bootstrapVersion,
    "org.typelevel"  %% "cats-core"                    % "2.10.0"
  // format: ON
  )

  val test = Seq(
  // format: OFF
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % "test, it"
  // format: ON
  )

  // only add additional dependencies here - it test inherit test dependencies above already
  val itDependencies: Seq[ModuleID] = Seq()

}
