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

package uk.gov.hmrc.datecalculator.models

import play.api.libs.json.{JsError, JsString, JsSuccess, Json, Reads}

import java.time.LocalDate

final case class AddWorkingDaysRequest(
    date:                     LocalDate,
    numberOfWorkingDaysToAdd: Int,
    regions:                  Set[Region]
)

object AddWorkingDaysRequest {

  implicit val regionReads: Reads[Region] =
    Reads{
      case JsString("EW")  => JsSuccess(Region.EnglandAndWales)
      case JsString("SC")  => JsSuccess(Region.Scotland)
      case JsString("NI")  => JsSuccess(Region.NorthernIreland)
      case JsString(other) => JsError(s"Could not read unknown region: $other")
      case other           => JsError(s"Expected JsString for region but got ${other.getClass.getSimpleName}")
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val reads: Reads[AddWorkingDaysRequest] = Json.reads

}
