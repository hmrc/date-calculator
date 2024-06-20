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

import com.google.inject.Inject
import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.util.Timeout
import uk.gov.hmrc.datecalculator.thing.ActorA.Requests.Start

import scala.concurrent.duration._

class Thing @Inject() (actorSystem: ActorSystem) {

  implicit val timeout: Timeout = Timeout(10.seconds)

  val actorA: ActorRef = actorSystem.actorOf(ActorA.props())

  //  actorA ! ActorA.Requests.Hello
  //  actorA ! "Bye"

  //  val result: Future[ActorA.Responses.HelloResponse.type] =
  //    (actorA ? ActorA.Requests.Hello).mapTo[ActorA.Responses.HelloResponse.type]

  //  println(Await.result(result, 10.seconds))
  actorA ! Start

}
