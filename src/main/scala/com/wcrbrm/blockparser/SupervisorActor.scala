package com.wcrbrm.blockparser

import java.io.{File, PrintWriter}

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import io.circe._
import io.circe.parser._

import scala.concurrent.ExecutionContext
import scala.sys.process
import scala.util.Properties

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
  import Packets._

  val buffer = new ListBuffer[ChainHeader]
  val folderAmounts: String = Properties.envOrElse("HASHES_FOLDER", "/tmp/cache/btc/amounts")

  def getHashDir(hash: String): String = {
    new StringBuffer(folderAmounts).append("/")
      .append(hash.substring( hash.length - 3 ) ).toString
  }
  def getHashFile(hash: String): File = {
    val sb = new StringBuffer(getHashDir(hash)).append( "/" ).append(hash).append(".json").toString
    new File(sb)
  }
  def saveHashFile(hash: String, contents: String): Unit = {
    val file = getHashFile(hash)
    log.debug("Saving HASH for {}, {} bytes", file.getAbsolutePath, contents)
    val dir = new File(getHashDir(hash))
    if (!dir.exists) dir.mkdirs
    new PrintWriter(file.getAbsolutePath) { write(contents); close() }
  }
  def isDownloaded(hash: String): Boolean = getHashFile(hash).exists

  override def receive = {

    case Enqueue(queue: List[ChainHeader]) =>
      log.info("Adding " + queue.length + " headers to the queue")
      queue.foreach(q => buffer += q)
      sender ! ReadyForMore

    case WorkerActor.NewWorker(index, server, ssh) =>
      val child = context.actorOf(WorkerActor.props(server, ssh), s"${server.ip}")
      child ! WorkerActor.UploadAgent

    case WorkerActor.GimmeWork =>
      println("GIMME WORK request from " + sender + ", " + buffer.size + " blocks left")
      var found = false
      while (!found) {
        if (buffer.isEmpty) {
          log.info("NO MORE WORK")
          found = true
          implicit val executionContext: ExecutionContext = context.dispatcher
          context.system.scheduler.scheduleOnce(10.seconds)(sender ! WorkerActor.WakeUp)
        } else {
          val theLast: ChainHeader = buffer.last
          buffer.remove(buffer.length - 1)
          // whether we already have the hash ready
          if (isDownloaded(theLast.hash)) {
            found = false
            // TODO: handle the amounts - from the local file
          } else {
            found = true
            val command = "/tmp/blockreader -hash=" + theLast.hash
            sender ! WorkerActor.WorkStart(command)
          }
        }
      }

    case WorkerActor.WorkComplete(result: Tuple2[String, Int]) =>

      val statusCode = result._2
      val stdout = result._1
      if (statusCode != 0) {
        log.error("Worker failed with status {}: {}", statusCode, stdout);
      } else {
        log.debug("Work complete {}", result)
        decode[AbstractPacket](stdout) match {
          case Left(errPacket) =>
            log.error("packet reading failure: {}", errPacket)

          case Right(packet: AbstractPacket) =>
            log.info("Packet classified as {}", packet)
            packet.packet match {
              case "amounts" =>
                decode[AmountsPacket](stdout) match {
                  case Left(err) =>
                    log.error("amounts packet: {}", err)
                  case Right(a) =>
                    saveHashFile(a.hash, stdout)
                    // TODO: add block handling
                }
            }
        }
      }
  }
}
