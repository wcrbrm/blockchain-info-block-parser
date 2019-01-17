package com.wcrbrm.blockparser

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

object SupervisorActor {
  def props(): Props = Props(new SupervisorActor)
  final case object ReadyForMore
  final case class Enqueue(queue: List[ChainHeader])
}
class SupervisorActor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Supervisor started")
  override def postStop(): Unit = log.info("Supervisor stopped")

  implicit val timeoutOnAsk = akka.util.Timeout(30.minutes) // timeout on actor ask
  import SupervisorActor._

  val buffer = new ListBuffer[ChainHeader]

  override def receive = {

    case Enqueue(queue: List[ChainHeader]) =>
      log.info("Adding " + queue.length + " headers")
      // must response
      queue.foreach(q => buffer += q)
      sender ! ReadyForMore

    case WorkerActor.NewWorker(index, server, ssh) =>
      val child = context.actorOf(WorkerActor.props(server, ssh), s"${index}")
      child ! WorkerActor.UploadAgent

    case WorkerActor.GimmeWork =>
      println("GIMME WORK request from " + sender)

      if (buffer.length == 0) {
        log.info("NO MORE WORK")
      } else {
        val theLast = buffer.last
        buffer.remove(buffer.length - 1)
      }

    case WorkerActor.WorkComplete(execResult: Tuple2[_, _]) =>
      println("Work complete" + execResult)
  }
}
