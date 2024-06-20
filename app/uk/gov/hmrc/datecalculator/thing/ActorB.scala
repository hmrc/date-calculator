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

package uk.gov.hmrc.datecalculator.thing

import org.apache.pekko.actor.{Actor, PoisonPill, Props}

class ActorB extends Actor {

  override def preStart(): Unit = {
    super.preStart()
    println("ActorB started")

    self ! PoisonPill
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case msg => println(s"ActorB received message: ${msg.toString}")
  }

}

object ActorB {

  def props(): Props = Props(new ActorB)

}
