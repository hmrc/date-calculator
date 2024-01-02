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

package uk.gov.hmrc.datecalculator.config

import com.google.inject.{AbstractModule, Provides, Singleton}
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import uk.gov.hmrc.datecalculator.services.WorkingDaysService

import java.time.{Clock, ZoneId}
class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
    bind(classOf[WorkingDaysService]).asEagerSingleton()
  }

  @Provides
  @Singleton
  def clock(): Clock = Clock.system(ZoneId.of("Europe/London"))

  @Provides
  @Singleton
  def scheduler(actorSystem: ActorSystem): Scheduler = actorSystem.scheduler

  @Provides
  @Singleton
  def workingDaysServiceStartUpHook: WorkingDaysService.StartUpHook = WorkingDaysService.StartUpHook(() => ())

}
