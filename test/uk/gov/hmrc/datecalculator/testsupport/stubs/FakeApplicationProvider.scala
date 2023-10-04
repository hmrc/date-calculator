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

import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalTime, ZoneId}

trait FakeApplicationProvider { this: GuiceFakeApplicationFactory with WireMockSupport =>

  val getBankHolidaysApiUrlPath = "/get-bank-holidays"

  val getBankHolidaysFromEmailAddress = "test@email.com"

  // make sure daily refreshes don't happen while specs is running - shouldn't take 24h for specs to run
  val bankHolidayDailyRefreshTime = {
    val clock = Clock.system(ZoneId.of("Europe/London"))
    LocalTime.now(clock).minusMinutes(1).format(DateTimeFormatter.ofPattern("HH:mm"))
  }

  val overrideModules: Seq[GuiceableModule] = Seq.empty

  val overrideConfig: Map[String, Any] = Map.empty

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "auditing.traceRequests" -> false,
        "metrics.enabled" -> false,
        "bank-holiday-api.url" -> s"http://localhost:${wireMockPort.toString}$getBankHolidaysApiUrlPath",
        "bank-holiday-api.from-email-address" -> getBankHolidaysFromEmailAddress,
        // make sure daily refresh doesn't happen while this spec is running - shouldn't take 24h for this spec to run
        "bank-holiday-api.daily-refresh-time" -> bankHolidayDailyRefreshTime,
      )
      .configure(overrideConfig)
      .overrides(overrideModules: _*)
      .build()

}
