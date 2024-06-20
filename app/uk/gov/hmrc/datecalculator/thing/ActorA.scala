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

import cats.Eq
import cats.syntax.eq._
import org.apache.pekko.actor.{Actor, ActorRef, Props, Terminated}
import uk.gov.hmrc.datecalculator.thing.ActorA.Requests.{Hello, Start}

class ActorA extends Actor {

  implicit val actorRefEq: Eq[ActorRef] = Eq.fromUniversalEquals[ActorRef]

  override def preStart(): Unit = {
    super.preStart()
    println("ActorA started")
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case Hello =>
      println("Hello from ActorA")
      sender() ! ActorA.Responses.HelloResponse

    case Start =>
      val child = context.watch(context.actorOf(ActorB.props()))
      context.become(withChild(child))

    case _ =>
      println("Unknown message")
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def withChild(child: ActorRef): Receive = {
    case Terminated(ref) =>
      if (ref === child) {
        println("Child terminated")
        context.become(receive)
      }

    case msg => child ! msg

  }

}

object ActorA {

  def props(): Props = Props(new ActorA)

  object Requests {
    case object Hello
    case object Start
  }

  object Responses {
    case object HelloResponse
  }

}
