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

package uk.gov.hmrc.datecalculator.testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, exactly, get, getRequestedFor, stubFor, urlPathEqualTo, verify}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object GDSStub {

  type HttpStatus = Int

  def stubGetBankHolidays(url: String, response: Either[HttpStatus, String]): StubMapping =
    stubFor(
      get(urlPathEqualTo(url))
        .willReturn(
          response match {
            case Left(status) => aResponse().withStatus(status)
            case Right(body)  => aResponse().withStatus(200).withBody(body)
          }
        )
    )

  def verifyGetBankHolidaysCalled(url: String, fromEmailAddress: String): Unit =
    verify(
      exactly(1),
      getRequestedFor(urlPathEqualTo(url))
        .withHeader("From", equalTo(fromEmailAddress))
    )

  def verifyGetBankHolidaysNotCalled(url: String): Unit =
    verify(exactly(0), getRequestedFor(urlPathEqualTo(url)))

  def getBankHolidaysApiResponseJsonString(
    englandAndWalesBankHolidays: Set[LocalDate] = Set.empty,
    scotlandBankHolidays:        Set[LocalDate] = Set.empty,
    northernIrelandBankHolidays: Set[LocalDate] = Set.empty
  ): String = {
    def toEvent(date: LocalDate): String =
      s"""
         |{
         |  "title": "Test bank holiday ${UUID.randomUUID().toString}",
         |  "date": "${date.format(DateTimeFormatter.ISO_DATE)}",
         |  "notes": "",
         |  "bunting": true
         |}
         |""".stripMargin

    s"""
       |{
       |  "england-and-wales": {
       |    "division": "england-and-wales",
       |    "events": [ ${englandAndWalesBankHolidays.map(toEvent).mkString(",")}  ]
       |  },
       |  "scotland": {
       |    "division": "scotland",
       |    "events": [ ${scotlandBankHolidays.map(toEvent).mkString(",")} ]
       |  },
       |  "northern-ireland": {
       |    "division": "northern-ireland",
       |    "events": [ ${northernIrelandBankHolidays.map(toEvent).mkString(",")} ]
       |  }
       |}
       |""".stripMargin
  }

}
