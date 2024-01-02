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

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.github.pjfanning.pekko.scheduler.mock.VirtualTime
import org.apache.pekko.actor.Scheduler
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.datecalculator.models.{AddWorkingDaysRequest, Region}
import uk.gov.hmrc.datecalculator.services.WorkingDaysService.StartUpHook
import uk.gov.hmrc.datecalculator.testsupport.stubs.{FakeApplicationProvider, GDSStub}
import uk.gov.hmrc.http.test.ExternalWireMockSupport

import scala.concurrent.duration._
import java.time.{Clock, LocalDate, LocalTime, ZoneId}
import scala.annotation.unused

// the tests in this spec depend on a specific ordering of events - take care
// when adding tests as changing one test may affect another one
class WorkingDaysSchedulingControllerSpec extends AnyFreeSpecLike
  with Matchers
  with GuiceOneAppPerSuite
  with ExternalWireMockSupport
  with FakeApplicationProvider {

  // set it up so that the time now is always 08:58:25 and the scheduled daily refresh time is
  // 10:00. The scheduler should not run the first refresh job until 1 hour, 1 minute and 35 seconds
  // have passed
  val timeNow = LocalTime.of(8, 58, 25)

  val dailyRefreshTime = LocalTime.of(10, 0)

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  var hasStarted = false

  val time = new VirtualTime

  override val overrideConfig: Map[String, Any] = Map(
    "bank-holiday-api.daily-refresh-time" -> "10:00"
  )

  override val overrideModules: Seq[GuiceableModule] = Seq(
    new AbstractModule {
      override def configure(): Unit = ()

      @Provides @Singleton @unused
      def clock(): Clock = {
        val europeLondonZoneId = ZoneId.of("Europe/London")
        Clock.fixed(LocalDate.now(europeLondonZoneId).atTime(timeNow).atZone(europeLondonZoneId).toInstant, europeLondonZoneId)
      }

      @Provides @Singleton @unused
      def scheduler(): Scheduler = time.scheduler

      @Provides @Singleton @unused
      def startUpHook(): StartUpHook = StartUpHook(() => hasStarted = true)

    }
  )

  lazy val controller = app.injector.instanceOf[WorkingDaysController]

  def eventually[A](timeout: FiniteDuration)(f: => A): A =
    Eventually.eventually(PatienceConfiguration.Timeout(Span(timeout.toSeconds, Seconds))) {
      f
    }

  "(set up - waiting for app to start)" in {
    eventually(5.seconds) {
      hasStarted shouldBe true
    }
  }

  "The daily refresh of bank holidays" - {

    "shouldn't happen while the configured time has not been reached yet" in {
      // take the time 1 millisecond before the scheduled job should run
      time.advance(1.hour + 1.minute + 34.seconds + 999.milliseconds)

      GDSStub.verifyGetBankHolidaysNotCalled(getBankHolidaysApiUrlPath)
    }

    "should happen once the configured time has been reached" in {
      GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Left(503))

      time.advance(1)

      eventually(1.second){
        GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
      }
    }

    "should not happen before 24 hours have passed since the last attempt" in {
      time.advance(24.hours - 1.millisecond)

      GDSStub.verifyGetBankHolidaysNotCalled(getBankHolidaysApiUrlPath)
    }

    "should happen again once 24 hours have passed" in {
      val bankHolidaysResponse =
        GDSStub.getBankHolidaysApiResponseJsonString(
          englandAndWalesBankHolidays = Set(LocalDate.of(2023, 10, 2), LocalDate.of(2023, 10, 4), LocalDate.of(2023, 12, 31))
        )

      GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right(bankHolidaysResponse))

      time.advance(1.millisecond)

      eventually(1.second) {
        GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
      }
    }

    "should result in the list of bank holidays being stored to use in working days calculation" in {
      // allow some time for the service to actually store the bank holidays
      eventually(1.second) {
        val addWorkingDaysRequest = AddWorkingDaysRequest(LocalDate.of(2023, 10, 3), 1, Set(Region.EnglandAndWales))
        val result = controller.addWorkingDays(FakeRequest().withBody(addWorkingDaysRequest))

        contentAsJson(result) shouldBe Json.parse("""{ "result": "2023-10-05" }""")
      }
    }

    "should happen again once another 24 hours have passed" in {
      val bankHolidaysResponse =
        GDSStub.getBankHolidaysApiResponseJsonString(
          englandAndWalesBankHolidays = Set(LocalDate.of(2023, 10, 2), LocalDate.of(2023, 10, 5), LocalDate.of(2023, 12, 31))
        )

      GDSStub.stubGetBankHolidays(getBankHolidaysApiUrlPath, Right(bankHolidaysResponse))

      time.advance(24.hours)

      eventually(1.second){
        GDSStub.verifyGetBankHolidaysCalled(getBankHolidaysApiUrlPath, getBankHolidaysFromEmailAddress)
      }
    }

    "should overwrite any existing bank holiday list if one already exists" in {
      eventually(1.second) {
        val result1 = controller.addWorkingDays(FakeRequest().withBody(
          AddWorkingDaysRequest(LocalDate.of(2023, 10, 3), 1, Set(Region.EnglandAndWales))
        ))
        contentAsJson(result1) shouldBe Json.parse("""{ "result": "2023-10-04" }""")
      }

      val result2 = controller.addWorkingDays(FakeRequest().withBody(
        AddWorkingDaysRequest(LocalDate.of(2023, 10, 3), 2, Set(Region.EnglandAndWales))
      ))
      contentAsJson(result2) shouldBe Json.parse("""{ "result": "2023-10-06" }""")
    }

  }

}
