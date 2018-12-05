// blockchain-info-block-parser

import $ivy.`com.lihaoyi::ujson:0.7.1`, ujson._
import $ivy.`com.typesafe.akka::akka-actor:2.5.16`
import $ivy.`com.typesafe.akka::akka-stream:2.5.16`
import $ivy.`com.lightbend.akka::akka-stream-alpakka-file:1.0-M1`

object Gzip {
  import java.io.FileInputStream
  import java.util.zip.GZIPInputStream
  def decompress(f: java.io.File): Option[String] =
    scala.util.Try {
      val inputStream = new GZIPInputStream(new FileInputStream(f))
      scala.io.Source.fromInputStream(inputStream).mkString
    }.toOption
}

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Sink, Source}
import akka.actor.{ Actor, ActorLogging, Props, ActorSystem }
import akka.stream.ActorMaterializer
import akka.pattern.ask

implicit val system = ActorSystem("walker")
implicit val mat = ActorMaterializer()
implicit val ec = system.dispatcher

// actor to keep date aggregation
object DateAggregator {
  final case class Collect(height: Int, date: String, values: Map[Int, Int])
  final case object PrintState
}
class DateAggregator extends Actor with ActorLogging {
  import DateAggregator._
  import scala.collection.mutable.HashMap
  val vals = new HashMap[String, HashMap[Int, Int]]()
  override def receive: Receive = {
    case Collect(height, date, values) => {
      // log.info(s"Date/${date} received ${values} (height ${height})")
      if (!vals.contains(date)) {
        vals(date) = new HashMap[Int, Int]()
      }
      for ((key,value) <- values) {
        if (!vals(date).contains(key)) vals(date)(key) = 0
        vals(date)(key) += value
      }
    }
    case PrintState => 
      for ((date, dateValues) <- vals) {
        println(date + " | " + dateValues)
      }
  }
}

case class Block(jsonStr: String) {
  import java.text.SimpleDateFormat
  import scala.collection.mutable.{ ArrayBuffer, LinkedHashMap }

  val json: Js.Value = ujson.read(jsonStr)
  val block: LinkedHashMap[String, Js.Value] = json("blocks").arr(0).obj
  val hash: String = block.get("hash").get.str
  val prev_block: String = block.get("prev_block").get.str
  val height: Int = block.get("height").get.num.toInt
  var dtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  val time: Long = block.get("time").get.num.toLong
  val timeIso: String = dtf.format(time * 1000)
  val dateIso: String = timeIso.substring(0, 10)
  val tx: ArrayBuffer[Js.Value] = block.get("tx").get.arr
  val maxValue: Int = 999

  // a little FP kung fu to show amounts spectre, with limitation on transaction amount
  val amounts: Map[Int, Int] = tx.
    flatMap(_.obj.get("out").get.arr).
    map(_.obj.get("value").get.num.toInt).
    filter(_ <= maxValue).filter(_ > 0).
    groupBy(x => x).filter{ case (k,v) => v.size > 1 }.mapValues(_.size)
}

val dtAggregator = system.actorOf(Props[DateAggregator])
object JsonFileReader {
  def props(blockname: String): Props = Props(new JsonFileReader(blockname))
  final case class ParseJson(json: String)
}
case class JsonFileReader(blockname: String) extends Actor with ActorLogging {
  import JsonFileReader._
  override def receive: Receive = {
    case ParseJson(json) => 
      // log.info(s"Reading JSON: ${json.length} bytes (from ${sender()})")
      val block = Block(json)
      val amountsInJson: String = upickle.default.writeJs(block.amounts).toString
      // log.info(s"[${block.timeIso}]\tdate=${block.dateIso}\theight=${block.height}\t${amountsInJson}")
      // dtAggregator ! DateAggregator.Collect(block.height, block.dateIso, block.amounts)
      sender() ! blockname
      self ! akka.actor.PoisonPill
  }
}

// actor to process json files
val folder = scala.util.Properties.envOrElse("BLOCKS_FOLDER", "./")
val dir = java.nio.file.Paths.get(folder)

val cores = Math.max(java.lang.Runtime.getRuntime.availableProcessors, 1)
def now = java.time.LocalTime.now.toString
val t0 = System.nanoTime
println(s"\n\n[${now}] STARTED in ${dir}, using ${cores} cores")

val res = Directory.walk(dir).
  filter(x => (x.toString.endsWith(".gz") || x.toString.endsWith(".json"))).
  // take(100).
  mapAsyncUnordered[String](cores) { file =>
    val filename = file.toString
    val blockname = filename.split("[\\\\/]").last.replace(".json", "").replace(".gz", "")
    import scala.concurrent.duration._
    implicit val timeout = akka.util.Timeout(60.minutes)

    if (filename.endsWith(".gz")) {
      println(s"[${now}] unzipping \t${filename} (${blockname})")
      val unzipped:Option[String] = Gzip.decompress(new java.io.File(filename))
      if (unzipped.isDefined) {
        val json = unzipped.get
        println(s"[${now}] unzipped \t${filename}\t${json.length} bytes")
        val actor = system.actorOf(JsonFileReader.props(blockname), blockname) 
        (actor ? JsonFileReader.ParseJson(json)).mapTo[String]
      } else {
        println(s"[${now}] unzip failure \t${filename}")
        scala.concurrent.Future { blockname }
      }
    } else {
      scala.concurrent.Future {
        if (filename.endsWith(".json")) {
          println(s"[${now}] processing json \t${blockname}")
          // todo... case when files are not zipped
        } else {
          println(s"[${now}] skipping \t${filename}")
        }
        blockname
      }
    }
  } 
  .runForeach(x => println(s"[${now}] finished block \t"))
  .onComplete(_ => {
    dtAggregator ! DateAggregator.PrintState
    println(s"[${now}] FINISHED in " + ((System.nanoTime - t0) * 10e-10) + "s\n")
  })
