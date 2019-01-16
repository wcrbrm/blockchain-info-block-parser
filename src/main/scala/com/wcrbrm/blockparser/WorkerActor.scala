package com.wcrbrm.blockparser
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import fr.janalyse.ssh._

object WorkerActor {
  def props(server: Server, conn: SSH): Props = Props(new WorkerActor(server, conn))
  final case object UploadAgent
  final case object GimmeWork
  final case class NewWorker(index: Int, server: Server, conn: SSH)
  final case class Work(command: String)
  final case class WorkComplete(result: Tuple2[_, _])
}

case class WorkerActor(server: Server, conn: SSH) extends Actor with ActorLogging {
  import WorkerActor._
  val agentFile = new java.io.File("./agent/blockreader")

  override def receive = {
    case UploadAgent =>
      if (!agentFile.exists) throw new Exception("Agent File was not found for uploading")
      println("[" + server.ip + "] connected, uploading")
      conn.scp { case scp =>
        scp.send(agentFile, "/tmp/blockreader")
        conn.execute("chmod +x /tmp/blockreader")
        println("[" + server.ip + "] " + conn.execute("uptime"))
      }
      sender ! GimmeWork
    case Work(work: String) =>
      sender ! WorkComplete(doWork(work))
  }
  def doWork(work: String): Tuple2[_, _] = {
    conn.executeWithStatus(work)
  }
}
