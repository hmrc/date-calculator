/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.datecalculator.testsupport

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultTestServerFactory, RunningServer}
import play.api.{Application, Mode}
import play.core.server.ServerConfig
import uk.gov.hmrc.http.test.WireMockSupport

trait ItSpec
  extends AnyFreeSpecLike
  with Matchers
  with GuiceOneServerPerSuite
  with WireMockSupport { self =>

  val testServerPort: Int = 19001

  val databaseName: String = "payments-email-verification-it"

  def conf: Map[String, Any] = Map(
    "mongodb.uri" -> s"mongodb://localhost:27017/$databaseName",
    "auditing.enabled" -> false,
    "auditing.traceRequests" -> false
  )

  //in tests use `app`
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(conf)
    .build()

  object TestServerFactory extends DefaultTestServerFactory {
    override protected def serverConfig(app: Application): ServerConfig = {
      val sc = ServerConfig(port    = Some(testServerPort), sslPort = Some(0), mode = Mode.Test, rootDir = app.path)
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))
    }
  }

  override implicit protected lazy val runningServer: RunningServer =
    TestServerFactory.start(app)

}
