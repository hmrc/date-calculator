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

import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.datecalculator.models.{AddWorkingDaysError, AddWorkingDaysRequest, AddWorkingDaysResponse}
import uk.gov.hmrc.datecalculator.services.WorkingDaysService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class WorkingDaysController @Inject() (
    workingDaysService: WorkingDaysService,
    cc:                 ControllerComponents
)(implicit ex: ExecutionContext)
  extends BackendController(cc) {

  val addWorkingDays: Action[AddWorkingDaysRequest] = Action(parse.json[AddWorkingDaysRequest]).async { implicit request =>
    workingDaysService.addWorkingDays(request.body).map {
      case Left(AddWorkingDaysError.NoRegionsInRequest) =>
        BadRequest("Request must contain at least one region")

      case Left(AddWorkingDaysError.CalculationBeyondKnownBankHolidays) =>
        UnprocessableEntity

      case Right(date) =>
        Ok(Json.toJson(AddWorkingDaysResponse(date)))
    }
  }

}
