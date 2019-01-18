package com.wcrbrm.blockparser

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import io.circe._
import io.circe.parser._

object SupervisorActor {
  def props(): Props = Props(new SupervisorActor)
  final case object ReadyForMore
  final case class Enqueue(queue: List[ChainHeader])
  final case class SaveAmounts(amounts: AmountsPacket)
}
class SupervisorActor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Supervisor started")
  override def postStop(): Unit = log.info("Supervisor stopped")

  implicit val timeoutOnAsk = akka.util.Timeout(30.minutes) // timeout on actor ask
  import SupervisorActor._

  val buffer = new ListBuffer[ChainHeader]

  override def receive = {

    case Enqueue(queue: List[ChainHeader]) =>
      log.info("Adding " + queue.length + " headers to the queue")
      queue.foreach(q => buffer += q)
      sender ! ReadyForMore

    case SaveAmounts(amounts: AmountsPacket) =>
      log.info("Saving amounts for {}", amounts.hash)
      // TODO: save amounts to some cache

    case WorkerActor.NewWorker(index, server, ssh) =>
      val child = context.actorOf(WorkerActor.props(server, ssh), s"${server.ip}")
      child ! WorkerActor.UploadAgent

    case WorkerActor.GimmeWork =>
      println("GIMME WORK request from " + sender)
      if (buffer.length == 0) {
        log.info("NO MORE WORK")
      } else {
        // TODO: check whether we already have the hash ready
        val theLast: ChainHeader = buffer.last
        buffer.remove(buffer.length - 1)
        val command = "/tmp/blockreader -hash=" + theLast.hash
        child ! WorkerActor.WorkStart(command)
      }

    case WorkerActor.WorkComplete(result: Tuple2[String, Int]) =>
      log.info("Work complete" + result)
      val stdout = result._1
      decode[AbstractPacket](stdout).right.map {
        case packet =>
          log.info("Packet classified as {}", packet)
          // decode[AmountsPacket](stdout).right.map {
          // }
      }

  }
}
