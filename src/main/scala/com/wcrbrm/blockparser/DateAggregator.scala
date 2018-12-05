package com.wcrbrm.blockparser

import scala.collection.mutable.TreeMap
import akka.actor.{ Actor, ActorLogging }

// actor to keep date aggregation
object DateAggregator {
  final case class Collect(height: Int, date: String, values: Map[Int, Int])
  final case object PrintState
}
class DateAggregator extends Actor with ActorLogging {
  import DateAggregator._
  
  val vals = new TreeMap[String, TreeMap[Int, Int]]()
  override def receive: Receive = {
    case Collect(height, date, values) => {
      // log.info(s"Date/${date} received ${values} (height ${height})")
      if (!vals.contains(date)) {
        vals(date) = new TreeMap[Int, Int]()
      }
      for ((key,value) <- values) {
        if (!vals(date).contains(key)) vals(date)(key) = 0
        vals(date)(key) += value
      }
    }
    case PrintState => {
      for ((date, dateValues) <- vals) {
         println(date + " | " + dateValues)
      }
      sender ! "ok"
    }
  }
}
