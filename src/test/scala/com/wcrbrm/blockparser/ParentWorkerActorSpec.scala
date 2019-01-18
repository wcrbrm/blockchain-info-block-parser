package com.wcrbrm.blockparser

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.testkit.{ImplicitSender, TestKit}
import com.wcrbrm.blockparser.ParentActor.CreateChildMsg
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object ChildActor {
  def props(id: String): Props = Props(new ChildActor(id))
}
class ChildActor(id: String) extends Actor with ActorLogging {
  override def preStart(): Unit = log.debug(s"WorkerActor ${id} started")
  override def postStop(): Unit = log.debug(s"WorkerActor ${id} stopped")
  // import WorkerActor._
  def receive = {
    case "ping" =>
      // "pong"
      val senderStr: String = sender.toString
      if (senderStr.contains("deadLetters"))
        throw new Exception("Sender is detected as dead letters")

      log.info(" attempt to properly detect sender={}", senderStr)
      sender ! "pong"
  }
}


object ParentActor {
    def props(): Props = Props(new ParentActor)
    final case object CreateChildMsg
}
class ParentActor extends Actor with ActorLogging {
    override def preStart(): Unit = log.debug("ParentActor started")
    override def postStop(): Unit = log.debug("ParentActor stopped")

    import ParentActor._
    implicit val timeoutOnAsk = akka.util.Timeout(5.seconds) // timeout on actor ask

    var creator: Option[ActorRef] = None
    def receive = {
      case CreateChildMsg =>
        val id = "1"
        creator = Some(sender)
        log.info(s"attempt to create child ${id}, initiated by {}", sender)
        val childActor = context.actorOf(ChildActor.props(id), s"worker${id}")
        childActor ! "ping"

      case s: String =>
        log.info(s"parent received string '{}' from {}", s, sender)
        creator.map { c =>
          log.info("sending 'done' to parent {}", c)
          c ! "done"
        }
    }
}

class MySpec() extends TestKit(ActorSystem("My")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

    override def afterAll: Unit = {
        TestKit.shutdownActorSystem(system)
    }

    "Parent actor" must {
        "create another actor" in {
            val echo = system.actorOf(Props[ParentActor], "parent")
            echo ! CreateChildMsg
            expectMsg("done")
        }

    }
}