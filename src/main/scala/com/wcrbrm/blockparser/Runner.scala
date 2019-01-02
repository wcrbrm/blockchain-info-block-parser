package com.wcrbrm.blockparser

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.actor.{ ActorSystem, Props }
import scala.concurrent.duration._
import okhttp3._

object Runner extends App {

    implicit val system = ActorSystem("btc-blocks")
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher
    val cores = Math.max(java.lang.Runtime.getRuntime.availableProcessors, 1)

    implicit val timeout = akka.util.Timeout(60.minutes) // timeout on actor ask
    def now = java.time.LocalTime.now.toString
    def secondsFrom = (t: Long) => (((System.nanoTime - t) * 10e-10) / 10) + "s"
    val t0 = System.nanoTime
    // actor to process json files
    val folder = scala.util.Properties.envOrElse("BLOCKS_FOLDER", "/opt/btc-blocks")
    val dir = java.nio.file.Paths.get(folder)

    println(s"\n\n[${now}] STARTED in ${dir.toAbsolutePath}, using ${cores} cores")
    def blockname = (filename: String) => filename.split("[\\\\/]").last.replace(".json", "").replace(".gz", "")

    /// getting latest block properties
    val urlLatestBlock = "https://blockchain.info/latestblock"
    val request = new okhttp3.Request.Builder().url(urlLatestBlock).build
    val response = new OkHttpClient().newCall(request).execute
    val mapInfo = LatestBlock(response.body.string).get
    println( "Latest Block Info: " + mapInfo.toString)


/*
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
*/

// TASKS: 1. "caching":
    // getLatest block
    // go back though all missing blocks, download them and save as json spectre
    // no not download what is already there.


}

