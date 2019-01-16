package com.wcrbrm.blockparser

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.actor.{ActorSystem, Props}
import fr.janalyse.ssh.{SSH, SSHIdentity, SSHOptions}

import scala.util.Properties
import scala.concurrent.duration._
import okhttp3._

object Runner extends App {

    implicit val system = ActorSystem("btc-blocks")
    implicit val mat = akka.stream.ActorMaterializer()
    implicit val ec = system.dispatcher
    implicit val timeoutOnAsk = akka.util.Timeout(30.minutes) // timeout on actor ask

    def now = java.time.LocalTime.now.toString
    def secondsFrom = (t: Long) => (((System.nanoTime - t) * 10e-10) / 10) + "s"
    val t0 = System.nanoTime

    val supervisor = system.actorOf(SupervisorActor.props(), "workers")
    val fileName = Properties.envOrElse("FILE_HEADERS", "")
    val headers = ChainHeader.getAll(fileName)

    import io.circe.parser._
    val serversJson = scala.io.Source.fromResource("servers.json").getLines.mkString("\n")
    val servers = decode[List[Server]](serversJson).right.getOrElse(Nil)

    def getSession(server: Server): SSH = {
        val settings = if (server.auth_method == "password") {
            SSHOptions( host = server.ip, username = server.auth_user, password = server.auth_password )
        } else if (server.auth_privateKey.isDefined) {
            val identity = SSHIdentity(server.auth_privateKey.get)
            SSHOptions( host = server.ip, username = server.auth_user, identities = List(identity) )
        } else {
            throw new Exception(s"Cannot get session for ${server.ip}")
        }
        SSH(settings)
    }

    val actors = servers.take(2).zipWithIndex.flatMap { case (server, index) =>
        try {
            val ssh = getSession(server)
            val actor = supervisor ? WorkerActor.NewWorker(index, server, ssh)
            Some(actor)
        } catch {
            case e: Throwable => println("[" + server.ip + "] ERROR: ", e)
                None
        }
    }
    println("please press ENTER to stop")
    try scala.io.StdIn.readLine
    finally system.terminate()

}

