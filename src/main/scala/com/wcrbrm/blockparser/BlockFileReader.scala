package com.wcrbrm.blockparser

import java.io.File
import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.ask

object BlockFileReader {
  def props(filename: String): Props = Props(new BlockFileReader(filename))
  final case object ProcessFile
  final case object ReadGzip
  final case object ReadSpectre
  final case class SaveSpectre(height: Int, dateIso:String, amounts: Map[Int, Int])
}
case class BlockFileReader(filename: String) extends Actor with ActorLogging {
  import BlockFileReader._
  
  val blockname = filename.split("[\\\\/]").last.replace(".json", "").replace(".gz", "")
  val spectre = filename.replace(filename.split("[\\\\/]").last, s"${blockname}.spectre")
  
  val actorJsonReader = context.system.actorOf(JsonFileReader.props(blockname), s"jsonreader-${blockname}")  

  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(60.minutes) // timeout on actor ask

  override def receive: Receive = {
    case ReadGzip => {
      //log.info(s"unzipping \t${filename} (${blockname})")
      val unzipped:Option[String] = Gzip.decompress(new File(filename))
      if (unzipped.isDefined) {
        val json = unzipped.get
        //log.info(s"unzipped \t${filename}\t${json.length} bytes")
        sender ! ((actorJsonReader ? JsonFileReader.ParseJson(json)).mapTo[String])
      } else {
        log.warning(s"unzip failure \t${filename}")
        sender ! blockname
      }
    }
    case SaveSpectre(height: Int, dateIso:String, amounts: Map[Int, Int]) => {
      // val f = new File(spectre)
      val s = ujson.Obj(
        "height" -> ujson.Num(height),
        "dateIso" -> ujson.Str(dateIso),
        "amounts" -> ujson.Str(amounts.mkString(",").replace(" -> ", ":"))
      )
      log.info(s"writing spectre: ${s}")
      sender ! blockname
    }
    case ReadSpectre => {
      log.info(s"restoring from spectre ${blockname}")
      sender ! blockname
    }
    case ProcessFile => {
      
      if (new File(spectre).exists) {
        (self ? ReadSpectre).mapTo[String]
      } else if (filename.endsWith(".gz")) {
        log.info(s"start ${blockname}")
        (self ? ReadGzip).mapTo[String]
        log.info(s"end ${blockname}")
      } else {
        log.info(s"skipping \t${filename}, neither .gz nor .spectre file was found")
      }
      sender ! blockname
      self ! akka.actor.PoisonPill
    }
  }
}
