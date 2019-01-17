package com.wcrbrm.blockparser

import akka.pattern.ask
import akka.actor.{ActorSystem, Props}
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.Sink

import scala.util.Properties
import scala.concurrent.duration._
import io.circe.parser._

import scala.concurrent.Future

object Runner extends App {

    implicit val system = ActorSystem("btc-blocks")
    implicit val mat = akka.stream.ActorMaterializer()
    implicit val ec = system.dispatcher
    implicit val timeoutOnAsk = akka.util.Timeout(30.minutes) // timeout on actor ask

    def now = java.time.LocalTime.now.toString
    def secondsFrom = (t: Long) => (((System.nanoTime - t) * 10e-10) / 10) + "s"
    val t0 = System.nanoTime

    val starter = system.actorOf(SupervisorActor.props(), "workers")
    val serversJson = scala.io.Source.fromResource("servers.json").getLines.mkString("\n")
    val servers = decode[List[Server]](serversJson).right.getOrElse(Nil)
    val actors = servers.take(3).zipWithIndex.flatMap { case (server, index) =>
        try {
            val ssh = Server.getSession(server)
            val actor = starter ? WorkerActor.NewWorker(index, server, ssh)
            Some(actor)
        } catch {
            case e: Throwable => println("[" + server.ip + "] ERROR: ", e)
                None
        }
    }
    if (actors.length == 0) throw new Error("FATAL ERROR: No Active connections")

    val folder = Properties.envOrElse("HEADERS_FOLDER", "/opt/block-headers")
    val dir = java.nio.file.Paths.get(folder)
    Directory.walk(dir)
      .map(_.toString) // convert from sun.nio.fs.WindowsPath$WindowsPathWithAttributes ?
      .filter(_.endsWith(".json"))
      .take(2)
      .mapAsyncUnordered(1){ file =>
          val jsonWithHeaders = scala.io.Source.fromFile(file).getLines.mkString("\n")
          val headers = decode[List[ChainHeader]](jsonWithHeaders).right.getOrElse(Nil)
          // println(file, headers.length)
          starter ? SupervisorActor.Enqueue(headers)
       }
      .runForeach(_ => Future { "ok" })
      .onComplete(_ => {
          println(s"[${now}] FINISHED in ${secondsFrom(t0)}\n")
      })
}

