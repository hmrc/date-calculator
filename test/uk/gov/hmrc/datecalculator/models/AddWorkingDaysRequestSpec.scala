/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.datecalculator.models

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.datecalculator.testsupport.Givens.jsResultCanEqual

import java.time.LocalDate

class AddWorkingDaysRequestSpec extends AnyFreeSpecLike, Matchers {

  "AddWorkingDaysRequest" - {

    "must have a reads instance that" - {

      "is able to read valid regions" in {
        Seq(
          "EW" -> Region.EnglandAndWales,
          "SC" -> Region.Scotland,
          "NI" -> Region.NorthernIreland
        ).foreach { (regionCode, expectedRegion) =>
          withClue(s"For code $regionCode and expected region ${expectedRegion.toString}") {
            val json =
              s"""{
                 |  "date": "2013-12-03",
                 |  "numberOfWorkingDaysToAdd": 13,
                 |  "regions": [ "$regionCode" ]
                 |}""".stripMargin

            val expectedRequest =
              AddWorkingDaysRequest(LocalDate.of(2013, 12, 3), 13, Set(expectedRegion))

            Json.parse(json).validate[AddWorkingDaysRequest] shouldBe JsSuccess(expectedRequest)
          }
        }
      }

      "fails when the region is not recognised" in {
        val json =
          s"""{
             |  "date": "2013-12-03",
             |  "numberOfWorkingDaysToAdd": 13,
             |  "regions": [ "abc" ]
             |}""".stripMargin

        Json.parse(json).validate[AddWorkingDaysRequest] shouldBe a[JsError]
      }

      "fails when the region is not a string" in {
        val json =
          s"""{
             |  "date": "2013-12-03",
             |  "numberOfWorkingDaysToAdd": 13,
             |  "regions": [ 1 ]
             |}""".stripMargin

        Json.parse(json).validate[AddWorkingDaysRequest] shouldBe a[JsError]
      }

    }

  }

}
