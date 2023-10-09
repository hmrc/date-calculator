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
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.datecalculator.testsupport.stubs.{FakeApplicationProvider, GDSStub}
import uk.gov.hmrc.http.test.ExternalWireMockSupport

class WorkingDaysControllerItSpec
  extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneServerPerSuite
  with ExternalWireMockSupport
  with FakeApplicationProvider {

  lazy val wsClient = app.injector.instanceOf[WSClient]

  lazy val baseUrl = s"http://localhost:$port"

  val getBankHolidaysApiResponse: String =
    """
      |{
      |  "england-and-wales": {
      |    "division": "england-and-wales",
      |    "events": [
      |      {
      |        "title": "Earliest bank holiday",
      |        "date": "0000-01-01",
      |        "notes": "",
      |        "bunting": true
      |      },
      |      {
      |        "title": "Latest bank holiday",
      |        "date": "9999-12-31",
      |        "notes": "",
      |        "bunting": true
      |      }
      |    ]
      |  },
      |  "scotland": {
      |    "division": "scotland",
      |    "events": [
      |      {
      |        "title": "Earliest bank holiday",
      |        "date": "0000-01-01",
      |        "notes": "",
      |        "bunting": true
      |      },
      |      {
      |        "title": "Latest bank holiday",
      |         "date": "9999-12-31",
      |         "notes": "",
      |         "bunting": true
      |      }
      |    ]
      |  },
      |  "northern-ireland": {
      |    "division": "northern-ireland",
      |    "events": [
      |      {
      |        "title": "Earliest bank holiday",
      |        "date": "0000-01-01",
      |        "notes": "",
      |        "bunting": true
      |      },
      |      {
      |        "title": "Latest bank holiday",
      |        "date": "9999-01-01",
      |        "notes": "",
      |        "bunting": true
      |      }
      |    ]
      |  }
      |}
      |""".stripMargin

  "POST /add-working-days" should {

    lazy val addWorkingsDaysUrl = s"$baseUrl/date-calculator/add-working-days"

      def makeRequest(requestBody: String) =
        wsClient
          .url(addWorkingsDaysUrl)
          .withHttpHeaders("Content-Type" -> "application/json")
          .post(requestBody)
          .futureValue

    val validRequestBody =
      """
        |{
        |  "date": "2023-09-26",
        |  "numberOfWorkingDaysToAdd": 1,
        |  "regions": [ "EW", "SC", "NI" ]
        |}
        |""".stripMargin

    val expectedResponse =
      Json.parse("""{ "result": "2023-09-27" }""")

    "respond with 400 status if the JSON in the request body cannot be parsed" in {
      val response = makeRequest("{}")

      response.status shouldBe 400
      GDSStub.verifyGetBankHolidaysNotCalled(getBankHolidaysApiUrlPath)
    }

    "respond with 400 status if the JSON in the request body does not contain any regions" in {
      val response =
        makeRequest(
          """
            |{
            |  "date": "2023-09-26",
            |  "numberOfWorkingDaysToAdd": 1,
            |  "regions": [ ]
            |}
            |""".stripMargin
        )

      response.status shouldBe 400
      GDSStub.verifyGetBankHolidaysNotCalled(getBankHolidaysApiUrlPath)
    }

    "respond with 400 status if the JSON in the request body contains an unknown region" in {
      val response =
        makeRequest(
          """
            |{
            |  "date": "2023-09-26",
            |  "numberOfWorkingDaysToAdd": 1,
            |  "regions": [ "EW", "XX" ]
            |}
            |""".stripMargin
        )

      response.status shouldBe 400
      GDSStub.verifyGetBankHolidaysNotCalled(getBankHolidaysApiUrlPath)
    }

    "respond with 400 status if the JSON in the request body contains a region which isn't a string" in {
      val response =
        makeRequest(
          """
            |{
            |  "date": "2023-09-26",
            |  "numberOfWorkingDaysToAdd": 1,
            |  "regions": [ 1 ]
            |}
            |""".stripMargin
        )

      response.status shouldBe 400
      GDSStub.verifyGetBankHolidaysNotCalled(getBankHolidaysApiUrlPath)
    }

    "respond with a 500 status if there is a problem calling the GDS bank holiday API and no bank holidays are stored yet" in {
      GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Left(503))

      val response = makeRequest(validRequestBody)

      response.status shouldBe 500
      GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
    }

    "respond with a 500 status if there is the GDS bank holiday API response cannot be parsed and no bank holidays are stored yet" in {
      GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right("{ }"))

      val response = makeRequest(validRequestBody)

      response.status shouldBe 500
      GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
    }

    "respond with a 200 status if the JSON in the request is valid and " in {
      GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right(getBankHolidaysApiResponse))

      val response = makeRequest(validRequestBody)

      response.status shouldBe 200
      response.json shouldBe expectedResponse
      GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
    }

    "not trigger another call to the bank holidays API if a list of bank holidays is already stored" in {
      GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right(getBankHolidaysApiResponse))

      val response = makeRequest(validRequestBody)

      response.status shouldBe 200
      response.json shouldBe expectedResponse
      GDSStub.verifyGetBankHolidaysNotCalled(getBankHolidaysApiUrlPath)
    }

  }

}
