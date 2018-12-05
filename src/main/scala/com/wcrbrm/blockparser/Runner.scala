package com.wcrbrm.blockparser

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.actor.{ ActorSystem, Props }
import scala.concurrent.duration._

object Runner extends App {

    implicit val system = ActorSystem("btc-blocks")
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher

    implicit val timeout = akka.util.Timeout(60.minutes) // timeout on actor ask

    // actor to process json files
    val folder = scala.util.Properties.envOrElse("BLOCKS_FOLDER", "./")
    val dir = java.nio.file.Paths.get(folder)
    val cores = Math.max(java.lang.Runtime.getRuntime.availableProcessors, 1)
    def now = java.time.LocalTime.now.toString
    def secondsFrom = (t: Long) => ((System.nanoTime - t) * 10e-10) + "s"
    def blockname = (filename: String) => filename.split("[\\\\/]").last.replace(".json", "").replace(".gz", "")

    val t0 = System.nanoTime
    println(s"\n\n[${now}] STARTED in ${dir}, using ${cores} cores")

    val res = Directory.walk(dir)
    .map(_.toString) // convert from sun.nio.fs.WindowsPath$WindowsPathWithAttributes ?
    .filter(x => (x.endsWith(".gz") || x.endsWith(".json")))
    .take(10)
    .mapAsyncUnordered[String](cores) { filename => 
        val actorBlockHandler = system.actorOf(BlockFileReader.props(filename), s"blockfile-${blockname(filename)}") 
        (actorBlockHandler ? BlockFileReader.ProcessFile).mapTo[String] 
    } 
    .runForeach(x => println(s"[${now}] block ${x} done\t"))
    .onComplete(_ => {
        system.actorOf(Props[DateAggregator])  ? DateAggregator.PrintState
        println(s"[${now}] FINISHED in ${secondsFrom(t0)}\n")
    })

}

