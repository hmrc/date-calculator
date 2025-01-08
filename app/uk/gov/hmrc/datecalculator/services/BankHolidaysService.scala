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

package uk.gov.hmrc.datecalculator.services

import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import uk.gov.hmrc.datecalculator.connectors.BankHolidaysConnector
import uk.gov.hmrc.datecalculator.models.{BankHoliday, BankHolidays}
import uk.gov.hmrc.datecalculator.services.BankHolidaysService.{Event, GDSBankHolidays}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankHolidaysService @Inject() (bankHolidaysConnector: BankHolidaysConnector)(using ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  def getBankHolidays()(using hc: HeaderCarrier): Future[BankHolidays] =
    bankHolidaysConnector.getBankHolidays().map { httpResponse =>
      if httpResponse.status == OK then
        httpResponse.json.validate[GDSBankHolidays] match {
          case JsSuccess(gdsBankHolidays, _) =>
            toBankHolidays(gdsBankHolidays)

          case JsError(e) =>
            throw new Exception(s"Could not parse response from GDS get bank holidays API: ${e.toString()}")
        }
      else
        logger.warn(s"Got http status ${httpResponse.status.toString} when calling the bank holiday API")
        throw UpstreamErrorResponse(
          message = "Could not retrieve bank holidays",
          statusCode = httpResponse.status,
          reportAs = INTERNAL_SERVER_ERROR
        )
    }

  private def toBankHolidays(gdsBankHolidays: GDSBankHolidays): BankHolidays = {
    def toBankHolidays(events: Seq[Event]): Set[BankHoliday] = events.map(event => BankHoliday(event.date)).toSet

    BankHolidays(
      toBankHolidays(gdsBankHolidays.`england-and-wales`.events),
      toBankHolidays(gdsBankHolidays.scotland.events),
      toBankHolidays(gdsBankHolidays.`northern-ireland`.events)
    )
  }

}

object BankHolidaysService {

  private final case class Event(date: LocalDate)

  private final case class RegionalResult(events: Seq[Event])

  private final case class GDSBankHolidays(
    `england-and-wales`: RegionalResult,
    scotland:            RegionalResult,
    `northern-ireland`:  RegionalResult
  )

  private given Reads[Event] = {
    implicit val dateReads: Reads[LocalDate] = Reads.localDateReads(DateTimeFormatter.ISO_DATE)
    Json.reads
  }

  private given Reads[RegionalResult] = Json.reads

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private given Reads[GDSBankHolidays] = Json.reads

}
