package com.wcrbrm.blockparser

import java.text.SimpleDateFormat
import scala.collection.mutable.{ ArrayBuffer, LinkedHashMap }
import ujson._
import scala.util.{ Try, Failure, Success }

object IntOption {
  def get(key: String): Option[Int] = {
    Try(sys.env(key)) match {
      case Failure(_) => None
      case Success(v) if v.isEmpty => None 
      case Success(v) => Some(v.toInt)
    }  
  } 
}

case class JsonBlock(jsonStr: String) {
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
  val maxValue: Option[Int] = IntOption.get("MAX_VALUE")
  val minValue: Option[Int] = IntOption.get("MIN_VALUE")

  // a little FP kung fu to show amounts spectre, with limitation on transaction amount
  val amounts: Map[Int, Int] = tx
    .flatMap(_.obj.get("out").get.arr)
    .map(_.obj.get("value").get.num.toInt)
    .filter(x => (!maxValue.isDefined || maxValue.get >= x))
    .filter(x => (!minValue.isDefined || minValue.get <= x))
    .groupBy(x => x).filter{ case (k,v) => v.size > 1 }.mapValues(_.size)
}

case class LatestBlock(jsonStr: String) {
  val json: Js.Value = ujson.read(jsonStr)
  val block: LinkedHashMap[String, Js.Value] = json.obj
  val height: Int = block.get("height").get.num.toInt
  val blockIndex: Int = block.get("block_index").get.num.toInt
  val time: Long = block.get("time").get.num.toLong
  val hash: String = block.get("hash").get.str
  def get() = {
    Map("height" -> height, "block_index" -> blockIndex, "time" -> time, "hash" -> hash)
  }
}
