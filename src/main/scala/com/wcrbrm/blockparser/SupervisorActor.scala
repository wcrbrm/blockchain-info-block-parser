package com.wcrbrm.blockparser

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import scala.concurrent.duration._

object SupervisorActor {
  def props(): Props = Props(new SupervisorActor)
  final case class Enqueue(queue: List[ChainHeader])
}
class SupervisorActor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Supervisor started")
  override def postStop(): Unit = log.info("Supervisor stopped")

  implicit val timeoutOnAsk = akka.util.Timeout(30.minutes) // timeout on actor ask
   override def receive = {

    case SupervisorActor.Enqueue(queue: List[ChainHeader]) =>
      println("adding " + queue.length + " headers")

    case WorkerActor.NewWorker(index, server, ssh) =>
      val child = context.actorOf(WorkerActor.props(server, ssh), s"${index}")
      child ? WorkerActor.UploadAgent

    case WorkerActor.GimmeWork =>
      println("Work requested", sender)

    case WorkerActor.WorkComplete(execResult: Tuple2[_, _]) =>
      println("Work complete" + execResult)
  }
}
