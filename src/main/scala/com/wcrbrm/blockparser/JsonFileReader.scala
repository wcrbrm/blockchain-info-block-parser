package com.wcrbrm.blockparser

import akka.actor.{ Actor, ActorLogging, Props }

object JsonFileReader {
  def props(blockname: String): Props = Props(new JsonFileReader(blockname))
  final case class ParseJson(json: String)
}
case class JsonFileReader(blockname: String) extends Actor with ActorLogging {
  import JsonFileReader._
  override def receive: Receive = {
    case ParseJson(json) => {
      log.info(s"Reading JSON: ${json.length} bytes (from ${sender()})")
      val block = JsonBlock(json)
      // (sender ? BlockFileReader.SaveSpectre(block.height, block.dateIso, block.amounts))

      val dtAggregator = context.system.actorOf(Props[DateAggregator])
      dtAggregator ! DateAggregator.Collect(block.height, block.dateIso, block.amounts)
      // self ! akka.actor.PoisonPill
    }
  }
}