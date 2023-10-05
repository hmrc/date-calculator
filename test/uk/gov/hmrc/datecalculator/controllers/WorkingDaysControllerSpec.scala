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

package uk.gov.hmrc.datecalculator.controllers

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import play.api.http.Status
import play.api.libs.json.JsSuccess
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.datecalculator.models.{AddWorkingDaysRequest, AddWorkingDaysResponse, Region}
import uk.gov.hmrc.datecalculator.testsupport.stubs.{FakeApplicationProvider, GDSStub}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.LocalDate
import scala.concurrent.Future

class WorkingDaysControllerSpec extends AnyFreeSpecLike
  with Matchers
  with GuiceOneServerPerTest
  with WireMockSupport
  with FakeApplicationProvider {

  class Context {

    lazy val controller = fakeApplication().injector.instanceOf[WorkingDaysController]

  }

  "POST /add-working-days must" - {

    "return a 500 when" - {

      "no bank holidays have been stored yet and the call to retrieve bank holidays fail" in new Context {
        GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Left(503))

        val request = AddWorkingDaysRequest(LocalDate.of(2023, 9, 26), 3, Set(Region.EnglandAndWales))
        val result = controller.addWorkingDays(FakeRequest().withBody(request))

        val exception = intercept[UpstreamErrorResponse](await(result))
        exception.statusCode shouldBe 503
        exception.reportAs shouldBe 500
      }

    }

    "return a 422 when" - {

      "the stored list of bank holidays is empty" in new Context {
        GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right(GDSStub.getBankHolidaysApiResponseJsonString()))

        val request = AddWorkingDaysRequest(LocalDate.of(2023, 9, 26), 3, Set(Region.EnglandAndWales))
        val result = controller.addWorkingDays(FakeRequest().withBody(request))

        status(result) shouldBe UNPROCESSABLE_ENTITY
        GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
      }

      /**
       * use this set up in the tests below
       *
       * Sep--22------23------24------25------26------27------28------29------30------31-Oct--01------02
       *     Fri     Sat     Sun     Mon     Tue     Wed     Thu     Fri     Sat     Sun     Mon     Tue
       *                              ^                       ^
       *                  earliest bank holiday            latest bank holiday
       */
      val getBankHolidaysApiResponse =
        GDSStub.getBankHolidaysApiResponseJsonString(englandAndWalesBankHolidays = Set(LocalDate.of(2023, 9, 25), LocalDate.of(2023, 9, 28)))

        def test(request: AddWorkingDaysRequest)(context: Context): Unit = {
          GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right(getBankHolidaysApiResponse))

          val result = context.controller.addWorkingDays(FakeRequest().withBody(request))

          status(result) shouldBe UNPROCESSABLE_ENTITY
          GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
        }

      "the date in the request is more than one day (excluding weekends) before the earliest known bank holiday and the number " +
        "of days to add is positive" in new Context {
          test(AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), 1, Set(Region.EnglandAndWales)))(this)

        }

      "the date in the request is equal to the earliest known bank holiday and the number of days to " +
        "add is negative" in new Context {
          test(AddWorkingDaysRequest(LocalDate.of(2023, 9, 25), -1, Set(Region.EnglandAndWales)))(this)
        }

      "the date in the request is equal to the latest known bank holiday and the number of days to " +
        "add is positive" in new Context {
          test(AddWorkingDaysRequest(LocalDate.of(2023, 9, 28), 1, Set(Region.EnglandAndWales)))(this)
        }

      "the date in the request is more than one day (excluding weekends) after the latest known bank holiday and the " +
        "number of days to add is negative" in new Context {
          test(AddWorkingDaysRequest(LocalDate.of(2023, 10, 1), -1, Set(Region.EnglandAndWales)))(this)
        }

      "the date in the request is within bounds but" - {

        "the result of the calculation would be before the earliest known bank holiday" in new Context {
          test(AddWorkingDaysRequest(LocalDate.of(2023, 9, 26), -100, Set(Region.EnglandAndWales)))(this)
        }

        "the result of the calculation would be after the latest known bank holiday" in new Context {
          test(AddWorkingDaysRequest(LocalDate.of(2023, 9, 27), 100, Set(Region.EnglandAndWales)))(this)
        }

      }

    }

    "return a 200 and return the correct result when" - {

      /**
       * Base tests on this setup:
       *
       * Sep--13----------14----------15--------16--------17--------18--------19--------20--------21--------22
       *     Wed         Thu         Fri       Sat       Sun       Mon       Tue       Wed       Thu       Fri
       *      ^                                                     ^         ^         ^                   ^
       *      ^                                                     ^         ^         ^                   ^
       *      ^                                                     ^         ^         ^                   ^
       *  earliest bank holiday in all regions                      ^         ^         ^                latest bank holiday in all regions
       *                                                            ^         ^         ^
       *                                                            ^         ^         ^
       *                                                            ^         ^        bank holiday in Northern Ireland only
       *                                                            ^         ^
       *                                                            ^         ^
       *                                                            ^         ^
       *            bank holiday in England and Wales only ---------+         +---------- bank holiday in Scotland only
       */
      val getBankHolidaysApiResponse =
        GDSStub.getBankHolidaysApiResponseJsonString(
          englandAndWalesBankHolidays = Set(LocalDate.of(2023, 9, 13), LocalDate.of(2023, 9, 18), LocalDate.of(2023, 9, 22)),
          scotlandBankHolidays        = Set(LocalDate.of(2023, 9, 13), LocalDate.of(2023, 9, 19), LocalDate.of(2023, 9, 22)),
          northernIrelandBankHolidays = Set(LocalDate.of(2023, 9, 13), LocalDate.of(2023, 9, 20), LocalDate.of(2023, 9, 22))
        )

        def test(
            request:            AddWorkingDaysRequest,
            expectedResultBody: AddWorkingDaysResponse
        )(context: Context) = {
          withClue(s"For ${request.toString}: ") {
            GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right(getBankHolidaysApiResponse))

            val fakeRequest = FakeRequest().withBody(request)
            val result: Future[Result] = context.controller.addWorkingDays(fakeRequest)

            status(result) shouldBe Status.OK
            contentAsJson(result).validate[AddWorkingDaysResponse] shouldBe JsSuccess(expectedResultBody)

            GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
          }
        }

      "the number of workings days to add is zero and" - {

        "the date in the request is the earliest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 13), 0, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 13))
          )(this)
        }

        "the date in the request is the latest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 22), 0, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 22))
          )(this)
        }

        "the date in the request is before the earliest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 1), 0, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 1))
          )(this)
        }

        "the date in the request is after the latest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 10, 1), 0, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 10, 1))
          )(this)
        }

        "the date in the request is a weekend" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 16), 0, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 16))
          )(this)
        }

        "the date in the request is a bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 18), 0, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 18))
          )(this)
        }

        "the date in the request is a working day within the bounds of the known bank holidays" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 0, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)
        }

      }

      "the calculation involves addition and" - {

        "goes over bank holidays and weekends for England and Wales" in new Context {
          // check bank holiday on 18th
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)

          // check 19th and 20th aren't counted as bank holidays
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 2, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 20))
          )(this)
        }

        "goes over bank holidays and weekends for Scotland" in new Context {
          // check 18th isn't counted as bank holiday
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 1, Set(Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 18))
          )(this)

          // check 19th is bank holiday and 20th isn't counted as bank holiday
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 2, Set(Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 20))
          )(this)
        }

        "goes over bank holidays and weekends for Northern Ireland" in new Context {
          // check 18th isn't counted as bank holiday
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 1, Set(Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 18))
          )(this)

          // check 19th isn't counted as bank holiday
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 2, Set(Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)

          // check 20th is a bank holiday
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 3, Set(Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 21))
          )(this)
        }

        "there are multiple regions in the request" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 1, Set(Region.EnglandAndWales, Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 20))
          )(this)

          // bank holiday in Scotland should be ignored, but bank holiday in England and Wales shouldn't be
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 1, Set(Region.EnglandAndWales, Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)

          // bank holidays in Northern Ireland and England and Wales should be counted
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 2, Set(Region.EnglandAndWales, Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 21))
          )(this)

          // bank holiday in England and Wales should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 1, Set(Region.Scotland, Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 18))
          )(this)

          // bank holidays in Scotland and Northern Ireland should be counted
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 15), 2, Set(Region.Scotland, Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 21))
          )(this)

        }

        "the date in the request is on a weekend" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 16), 1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)
        }

        "the date in the request is a bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 18), 1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)
        }

        "the date in the request is the earliest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 13), 2, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)
        }

        "the date in the request is one day before (excluding weekends) the earliest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 12), 1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 14))
          )(this)
        }

      }

      "the calculation involves subtraction and" - {

        "the calculation involves subtraction and goes over bank holidays and weekends" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 19), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)
        }

        "goes over bank holidays and weekends for England and Wales" in new Context {
          // 20th should be counted as bank holiday
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 19), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)

          // bank holiday in Scotland should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 20), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)

          // bank holiday in Northern Ireland should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 20))
          )(this)
        }

        "goes over bank holidays and weekends for Scotland" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 20), -2, Set(Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)

          // bank holiday in England and Wales should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 20), -1, Set(Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 18))
          )(this)

          // bank holiday in Northern Ireland should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -1, Set(Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 20))
          )(this)
        }

        "goes over bank holidays and weekends for Northern Ireland" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -3, Set(Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)

          // bank holiday in Scotland should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -1, Set(Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)

          // bank holiday in England and Wales and Scotland should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -2, Set(Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 18))
          )(this)
        }

        "there are multiple regions in the request" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 20), -1, Set(Region.EnglandAndWales, Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)

          // bank holiday in Northern Ireland should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -1, Set(Region.EnglandAndWales, Region.Scotland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 20))
          )(this)

          // bank holiday in Scotland should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -1, Set(Region.EnglandAndWales, Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 19))
          )(this)

          // bank holiday in Northern Ireland and England shouldn't be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -2, Set(Region.EnglandAndWales, Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 15))
          )(this)

          // bank holiday in England should be ignored
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 21), -1, Set(Region.Scotland, Region.NorthernIreland)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 18))
          )(this)

        }

        "the date in the request is on a weekend" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 17), -2, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 14))
          )(this)
        }

        "the date in the request is a bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 18), -2, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 14))
          )(this)
        }

        "the date in the request is the latest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 22), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 21))
          )(this)
        }

        "the date in the request is one day after (excluding weekends) the latest bank holiday" in new Context {
          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 23), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 21))
          )(this)

          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 24), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 21))
          )(this)

          test(
            AddWorkingDaysRequest(LocalDate.of(2023, 9, 25), -1, Set(Region.EnglandAndWales)),
            AddWorkingDaysResponse(LocalDate.of(2023, 9, 21))
          )(this)
        }

      }

    }

  }
}
