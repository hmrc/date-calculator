import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.22.0"

  val compile = Seq(
  // format: OFF
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion
  // format: ON
  )

  val test = Seq(
  // format: OFF
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % "test, it"
  // format: ON
  )
}
