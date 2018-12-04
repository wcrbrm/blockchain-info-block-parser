// blockchain-info-block-parser

import $ivy.`com.lihaoyi::ujson:0.7.1`, ujson._
import $ivy.`com.typesafe.akka::akka-actor:2.5.16`
import $ivy.`com.typesafe.akka::akka-stream:2.5.16`
import $ivy.`com.lightbend.akka::akka-stream-alpakka-file:1.0-M1`

object Gzip {
  import scala.util.Try
  def decompress(compressed: Array[Byte]): Option[String] =
    Try {
      val inputStream = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(compressed))
      scala.io.Source.fromInputStream(inputStream).mkString
    }.toOption
}

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Sink, Source}
import akka.actor.{ Actor, ActorLogging, Props, ActorSystem }
import akka.stream.ActorMaterializer

implicit val system = ActorSystem("walker")
implicit val mat = ActorMaterializer()
implicit val ec = system.dispatcher

// actor to keep date aggregation
object DateAggregation {
  def props(date: String): Props = Props(new DateAggregation(date))
  final case class Collect(values: Map[Int, Int])
}
case class DateAggregation(date: String) extends Actor with ActorLogging {
  import DateAggregation._
  import scala.collection.mutable.HashMap

  val vals = new HashMap[Int, Int]()
  override def preStart(): Unit = log.info("DateAggregation actor {} started", date)
  override def postStop(): Unit = log.info("DateAggregation actor {} stopped", date)
  override def receive: Receive = {
    case Collect(values) => 
      log.info(s"DateAggregation ${date} Collect received {values} (from ${sender()})")
  }
}

object JsonFileReader {
  def props(filename: String): Props = Props(new JsonFileReader(filename))
  final case class Read(json: String)
}
case class JsonFileReader(filename: String) extends Actor with ActorLogging {
  import JsonFileReader._
  override def preStart(): Unit = log.info("JsonFileReader actor '{}' started", filename)
  override def postStop(): Unit = log.info("JsonFileReader actor '{}' stopped", filename)

  override def receive: Receive = {
    case Read(json) => 
      log.info(s"Reading JSON: ${json.length} bytes (from ${sender()})")
  }

  def parseBlock(f: java.io.File): Map[Int, Int] = {
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
    amounts
  }
}

object BlockFileReader {
  case object Ack
  case object Init
  case object Completed
  final case class Failure(ex: Throwable)
}
class BlockFileReader extends Actor with ActorLogging {
  import BlockFileReader._
  override def receive: Receive = {
    // case filename: String => 
    //   log.info(s"BlockFileReader received '${filename}' (from ${sender()})")
    //   sender() ! Ack // ack to allow the stream to proceed sending more elements
    case Init =>
      log.info("Stream initialized ${filename}")
      sender() ! Ack // ack to allow the stream to proceed sending more elements
    case Completed =>
      log.info("Stream completed!")
    case Failure(ex) =>
      log.error(ex, "Stream failed!")
  }
}


// actor to process json file

val folder = scala.util.Properties.envOrElse("FOLDER", "./")
val dir = java.nio.file.Paths.get(folder)

// val actorDateAggregation = system.actorOf(DateAggregation.props("2018-02-01"))
// val actorJsonFileReader = system.actorOf(JsonFileReader.props("example"))

val actorReader = system.actorOf(Props[BlockFileReader], "block-file-reader")
val sink = Sink.actorRefWithAck(
  actorReader,
  onInitMessage = BlockFileReader.Init,
  ackMessage = BlockFileReader.Ack,
  onCompleteMessage = BlockFileReader.Completed,
  onFailureMessage = BlockFileReader.Failure
)

println("STARTED in " + dir)
Directory.walk(dir).
  filter(_.toString.endsWith(".json")).
  take(5).
  //runWith(sink)
  runForeach(println).
  onComplete(_ => println("FINISHED"))
