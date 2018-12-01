// blockchain-info-block-parser

import $ivy.`com.lihaoyi::ujson:0.7.1`, ujson._
import $ivy.`com.typesafe.akka::akka-actor:2.5.16`
import $ivy.`com.typesafe.akka::akka-stream:2.5.16`
import $ivy.`com.lightbend.akka::akka-stream-alpakka-file:1.0-M1`

def parseBlock(f: java.io.File) {
  import java.text.SimpleDateFormat
  import scala.collection.mutable.{ ArrayBuffer, LinkedHashMap }

  val json: Js.Value = read(f)
  val block: LinkedHashMap[String, Js.Value] = json("blocks").arr(0).obj
  val hash: String = block.get("hash").get.str
  val prev_block: String = block.get("prev_block").get.str
  val height: Int = block.get("height").get.num.toInt
  var dtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  val time: Long = block.get("time").get.num.toLong
  val timeIso: String = dtf.format(time * 1000)
  val tx: ArrayBuffer[Js.Value] = block.get("tx").get.arr
  val maxValue: Int = 999

  // a little FP kung fu to show amounts spectre, with limitation on transaction amount
  val amounts: Map[Int, Int] = tx.
     flatMap(_.obj.get("out").get.arr).
     map(_.obj.get("value").get.num.toInt).
     filter(_ <= maxValue).filter(_ > 0).
     groupBy(x => x).filter{ case (k,v) => v.size > 1 }.mapValues(_.size)
   
  val amountsInJson: String = upickle.default.writeJs(amounts).toString
  println(s"[${timeIso}] ${height} ${amountsInJson}")
}


import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Sink, Source}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

val dir = java.nio.file.Paths.get("./")
implicit val system = ActorSystem("files-walker")
implicit val mat = ActorMaterializer()

Directory.walk(dir).filter(_.toString.endsWith("json")).runForeach(println)


