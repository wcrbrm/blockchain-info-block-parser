package com.wcrbrm.blockparser
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import fr.janalyse.ssh._

object WorkerActor {
  def props(server: Server, conn: SSH): Props = Props(new WorkerActor(server, conn))
  final case object UploadAgent
  final case object GimmeWork
  final case class NewWorker(index: Int, server: Server, conn: SSH)
  final case class WorkStart(command: String)
  final case class WorkComplete(result: Tuple2[_, _])
}

case class WorkerActor(server: Server, conn: SSH) extends Actor with ActorLogging {
  import WorkerActor._
  val agentFile = new java.io.File("./agent/blockreader")

  override def preStart(): Unit = log.info("Worker actor started {}", server.ip)
  override def postStop(): Unit = log.info("Worker actor stopped {}", server.ip)

  override def receive = {
    case UploadAgent =>
      if (!agentFile.exists) throw new Exception("Agent File was not found for uploading")

      // WARNING: sender is wrong
      log.info("[" + server.ip + "] connected, uploading. sender={}", sender())
      conn.scp { case scp =>
        scp.send(agentFile, "/tmp/blockreader")
        conn.execute("chmod +x /tmp/blockreader")
        log.info("[" + server.ip + "] " + conn.execute("uptime") + " for " + sender() )
      }
      sender ! GimmeWork
    case Work(work: String) =>
      sender ! WorkComplete(conn.executeWithStatus(work))
      // sender ! GimmeWork
  }
}
