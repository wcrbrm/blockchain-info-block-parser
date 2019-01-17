package com.wcrbrm.blockparser

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.wcrbrm.blockparser.ParentActor.CreateChildMsg
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object WorkerActor {
  def props(id: String): Props = Props(new WorkerActor(id))
}
class WorkerActor(id: String) extends Actor with ActorLogging {
  override def preStart(): Unit = log.info(s"WorkerActor ${id} started")
  override def postStop(): Unit = log.info("WorkerActor  ${id}stopped")
  // import WorkerActor._
  def receive = {
    case "ping" =>
      // "pong"
      val senderStr: String = sender.toString
      if (senderStr.contains("deadLetters"))
        throw new Exception("Sender is detected as dead letters")

      log.info(" attempt to properly detect sender={}", senderStr)
  }
}


object ParentActor {
    def props(): Props = Props(new ParentActor)
    final case object CreateChildMsg
}
class ParentActor extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("ParentActor started")
    override def postStop(): Unit = log.info("ParentActor stopped")

    import ParentActor._
    def receive = {
      case CreateChildMsg =>
        val id = "12312312312"
        log.info(s"attempt to create child ${id}")
        val actor = context.actorOf(WorkerActor.props(id), s"worker${id}")
        actor ! "ping"
    }
}

class MySpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

    override def afterAll: Unit = {
        TestKit.shutdownActorSystem(system)
    }
    implicit val timeoutOnAsk = akka.util.Timeout(30.minutes) // timeout on actor ask

    "Parent actor" must {
        "create another actor" in {
            val echo = system.actorOf(Props[ParentActor], "parent")
            echo ? CreateChildMsg
            // expectMsg("hello world")
        }

    }
}