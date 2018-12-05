package com.wcrbrm.blockparser

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.ask
import scala.concurrent.duration._

final case class Spectre(height: Int, dateIso: String, amounts: Map[Int, Int])

object JsonReaderActor {
  def props(blockname: String): Props = Props(new JsonReaderActor(blockname))
  final case class ParseJson(json: String)
}
case class JsonReaderActor(blockname: String) extends Actor with ActorLogging {
  import JsonReaderActor._
  // implicit val timeout = akka.util.Timeout(30.seconds)

  override def receive: Receive = {
    case ParseJson(json) => {
      log.info(s"Reading JSON: ${json.length} bytes (from ${sender()})")
      val block = JsonBlock(json)
      log.info(s"Sending back ${block.height} ${block.dateIso} ${block.amounts} to ${sender().path}")
      sender() ! Spectre(block.height, block.dateIso, block.amounts)

      // val dtAggregator = context.system.actorOf(Props[DateAggregator])
      // dtAggregator ! DateAggregator.Collect(block.height, block.dateIso, block.amounts)

      self ! akka.actor.PoisonPill
    }
  }
}