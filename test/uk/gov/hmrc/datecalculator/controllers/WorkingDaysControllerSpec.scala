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

import akka.stream.Materializer
import play.api.http.Status
import play.api.libs.json.JsSuccess
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.datecalculator.models.{AddWorkingDaysRequest, AddWorkingDaysResponse}
import uk.gov.hmrc.datecalculator.testsupport.ItSpec

import java.time.LocalDate
import scala.concurrent.Future

class WorkingDaysControllerSpec extends ItSpec {

  lazy val controller = fakeApplication().injector.instanceOf[WorkingDaysController]

  implicit lazy val mat: Materializer = fakeApplication().injector.instanceOf[Materializer]

  "POST /add-working-days must" - {

    "return a 200 when" - {

        def test(request: AddWorkingDaysRequest, expectedResultBody: AddWorkingDaysResponse) = {
          val fakeRequest = FakeRequest().withBody(request)
          val result: Future[Result] = controller.addWorkingDays(fakeRequest)

          println(contentAsString(result))
          status(result) shouldBe Status.OK
          contentAsJson(result).validate[AddWorkingDaysResponse] shouldBe JsSuccess(expectedResultBody)
        }

      "the number of days to add is positive" in {
        test(
          AddWorkingDaysRequest(LocalDate.of(2023, 9, 26), 3),
          AddWorkingDaysResponse(LocalDate.of(2023, 9, 29))
        )
      }

      "the number of days to add is zero" in {
        test(
          AddWorkingDaysRequest(LocalDate.of(2023, 9, 26), 0),
          AddWorkingDaysResponse(LocalDate.of(2023, 9, 26))
        )
      }

      "the number of days to add is negative" in {
        test(
          AddWorkingDaysRequest(LocalDate.of(2023, 9, 26), -4),
          AddWorkingDaysResponse(LocalDate.of(2023, 9, 22))
        )
      }
    }

  }
}
