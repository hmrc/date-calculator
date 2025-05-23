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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.datecalculator.testsupport.Givens.{jsResultCanEqual, jsValueCanEqual}

import java.time.LocalDate

class AddWorkingDaysResponseSpec extends AnyWordSpecLike, Matchers {

  "AddWorkingDaysResponse" must {

    "have a format instance" in {
      val response     = AddWorkingDaysResponse(LocalDate.of(2020, 12, 14))
      val expectedJson = Json.parse("""{ "result": "2020-12-14" }""")

      // test the writes
      Json.toJson(response) shouldBe expectedJson
      // test the reads
      expectedJson.validate[AddWorkingDaysResponse] shouldBe JsSuccess(response)

    }

  }

}
