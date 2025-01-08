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

package uk.gov.hmrc.datecalculator.connectors

import com.google.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import uk.gov.hmrc.datecalculator.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankHolidaysConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(using ExecutionContext) {

  def getBankHolidays()(using HeaderCarrier): Future[HttpResponse] =
    httpClient
      .get(url"${appConfig.bankHolidaysApiUrl}")
      .setHeader(HeaderNames.FROM -> appConfig.bankHolidaysApiFromEmailAddress)
      .withProxy
      .execute

}
