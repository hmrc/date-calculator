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

import akka.actor.{Cancellable, Scheduler}
import cats.syntax.eq._
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.datecalculator.config.AppConfig
import uk.gov.hmrc.datecalculator.models.{AddWorkingDaysError, AddWorkingDaysRequest, BankHoliday, BankHolidays, Region}
import uk.gov.hmrc.datecalculator.services.WorkingDaysService.StartUpHook
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate, LocalTime}
import java.time.temporal.ChronoField
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Singleton
class WorkingDaysService @Inject() (
    appConfig:            AppConfig,
    bankHolidaysService:  BankHolidaysService,
    scheduler:            Scheduler,
    clock:                Clock,
    startUpHook:          StartUpHook,
    applicationLifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext) {

  import WorkingDaysService.LocalDateOps

  private val logger: Logger = Logger(this.getClass)

  private val twentyFourHoursInSeconds: Long = 24.hours.toSeconds

  private val maybeBankHolidays: AtomicReference[Option[BankHolidays]] = new AtomicReference(None)

  private val bankHolidaysDailyRefreshJob: Cancellable = {
    val timeUntilNextRefresh = timeUntil(appConfig.dailyRefreshTime)

    val timeString = {
      val hours = timeUntilNextRefresh.toHours
      val minutes = (timeUntilNextRefresh - hours.hours).toMinutes
      val seconds = (timeUntilNextRefresh - hours.hours - minutes.minutes).toSeconds
      s"${hours.toString}h${minutes.toString}m${seconds.toString}s"
    }
    logger.info(s"Scheduling next bank holidays daily refresh for ${appConfig.dailyRefreshTime.toString} in $timeString")

    scheduler.scheduleWithFixedDelay(
      timeUntilNextRefresh,
      24.hours
    )(new Runnable {
        override def run(): Unit =
          getAndSetBankHolidays()(HeaderCarrier()).onComplete {
            case Failure(e) => logger.warn(s"Could not refresh bank holidays", e)
            case Success(_) => logger.info(s"Successfully refreshed bank holidays")
          }
      })
  }

  private def timeUntil(t: LocalTime): FiniteDuration = {
    val now = LocalTime.now(clock)

    val seconds = {
      val delta = now.until(t, java.time.temporal.ChronoUnit.SECONDS)
      if (delta < 0) {
        twentyFourHoursInSeconds + delta
      } else {
        delta
      }
    }

    seconds.seconds
  }

  startUpHook.onStart()

  applicationLifecycle.addStopHook(() => Future.successful(bankHolidaysDailyRefreshJob.cancel()))

  def addWorkingDays(request: AddWorkingDaysRequest)(implicit hc: HeaderCarrier): Future[Either[AddWorkingDaysError, LocalDate]] =
    if (request.regions.isEmpty)
      Future.successful(Left(AddWorkingDaysError.NoRegionsInRequest))
    else
      for {
        bankHolidays <- maybeBankHolidays.get().fold(getAndSetBankHolidays())(Future.successful)
        result <- Future.successful(calculateWorkingDays(request, bankHolidays))
      } yield result

  private def calculateWorkingDays(
      request:      AddWorkingDaysRequest,
      bankHolidays: BankHolidays
  ): Either[AddWorkingDaysError, LocalDate] = {
    val relevantBankHolidays = getRelevantBankHolidays(request, bankHolidays)

    (relevantBankHolidays.minOption, relevantBankHolidays.maxOption) match {
      case (Some(earliestKnownBankHoliday), Some(latestKnownBankHoliday)) =>
        if (request.numberOfWorkingDaysToAdd === 0) Right(request.date)
        else nWorkingDaysFrom(request.date, request.numberOfWorkingDaysToAdd, relevantBankHolidays, earliestKnownBankHoliday, latestKnownBankHoliday)

      case _ =>
        Left(AddWorkingDaysError.CalculationBeyondKnownBankHolidays)
    }
  }

  private def getRelevantBankHolidays(
      request:      AddWorkingDaysRequest,
      bankHolidays: BankHolidays
  ): Set[BankHoliday] = {
    val englandAndWalesBankHolidays =
      if (request.regions.contains(Region.EnglandAndWales)) bankHolidays.englandAndWales else Set.empty
    val scotlandBankHolidays =
      if (request.regions.contains(Region.Scotland)) bankHolidays.scotland else Set.empty
    val northernIrelandBankHolidays =
      if (request.regions.contains(Region.NorthernIreland)) bankHolidays.northernIreland else Set.empty

    englandAndWalesBankHolidays ++ scotlandBankHolidays ++ northernIrelandBankHolidays
  }

  @tailrec
  private def nWorkingDaysFrom(
      date:                     LocalDate,
      n:                        Int,
      bankHolidays:             Set[BankHoliday],
      earliestKnownBankHoliday: BankHoliday,
      latestKnownBankHoliday:   BankHoliday
  ): Either[AddWorkingDaysError, LocalDate] = {
    val signum: Int = n.sign

    if (signum === 0) {
      if (date.isAfterOrEqualTo(latestKnownBankHoliday.date) || date.isBeforeOrEqualTo(earliestKnownBankHoliday.date))
        Left(AddWorkingDaysError.CalculationBeyondKnownBankHolidays)
      else
        Right(date)
    } else {
      val adjustedDate = date.plusDays(signum)
      val dayNumber: Int = adjustedDate.get(ChronoField.DAY_OF_WEEK)
      val isWeekend = dayNumber === 6 || dayNumber === 7
      val isBankHoliday: Boolean = bankHolidays.contains(BankHoliday(adjustedDate))
      val isWorkingDay = !isBankHoliday && !isWeekend

      nWorkingDaysFrom(
        adjustedDate,
        if (isWorkingDay) n - signum else n,
        bankHolidays,
        earliestKnownBankHoliday,
        latestKnownBankHoliday
      )
    }
  }

  private def getAndSetBankHolidays()(implicit hc: HeaderCarrier): Future[BankHolidays] =
    bankHolidaysService.getBankHolidays().map{ bankHolidays =>
      maybeBankHolidays.set(Some(bankHolidays))
      bankHolidays
    }

}

object WorkingDaysService {

  private implicit class LocalDateOps(private val d: LocalDate) extends AnyVal {

    def isBeforeOrEqualTo(other: LocalDate): Boolean = d.isBefore(other) || d.isEqual(other)

    def isAfterOrEqualTo(other: LocalDate): Boolean = d.isAfter(other) || d.isEqual(other)

  }

  final case class StartUpHook(onStart: () => Unit)

}
