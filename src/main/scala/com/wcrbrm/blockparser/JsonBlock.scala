package com.wcrbrm.blockparser

import java.text.SimpleDateFormat
import scala.collection.mutable.{ ArrayBuffer, LinkedHashMap }
import ujson._

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
  val maxValue: Int = 999

  // a little FP kung fu to show amounts spectre, with limitation on transaction amount
  val amounts: Map[Int, Int] = tx.
    flatMap(_.obj.get("out").get.arr).
    map(_.obj.get("value").get.num.toInt).
    filter(_ <= maxValue).filter(_ > 0).
    groupBy(x => x).filter{ case (k,v) => v.size > 1 }.mapValues(_.size)
}