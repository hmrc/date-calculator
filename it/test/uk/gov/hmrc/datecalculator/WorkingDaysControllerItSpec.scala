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

package uk.gov.hmrc.datecalculator

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

class WorkingDaysControllerItSpec
  extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneServerPerSuite {

  val wsClient = app.injector.instanceOf[WSClient]
  val baseUrl = s"http://localhost:$port"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .build()

  "POST /add-working-days" should {

    val addWorkingsDaysUrl = s"$baseUrl/date-calculator/add-working-days"

    "respond with 400 status if the JSON in the request body cannot be parsed" in {
      val response =
        wsClient
          .url(addWorkingsDaysUrl)
          .withHttpHeaders("Content-Type" -> "application/json")
          .post("{}")

          .futureValue

      response.status shouldBe 400
    }

    "respond with a 200 status if the JSON in the request is valid" in {
      val requestBody =
        """
          |{
          |  "date": "2023-09-26",
          |  "numberOfWorkingDaysToAdd": 1
          |}
          |""".stripMargin

      val expectedResponse =
        Json.parse("""{ "result": "2023-09-27"  }""")

      val response =
        wsClient
          .url(addWorkingsDaysUrl)
          .withHttpHeaders("Content-Type" -> "application/json")
          .post(requestBody)

          .futureValue

      response.status shouldBe 200
      response.json shouldBe expectedResponse
    }

  }

}
