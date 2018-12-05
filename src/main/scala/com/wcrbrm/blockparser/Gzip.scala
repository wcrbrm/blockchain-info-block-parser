package com.wcrbrm.blockparser
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

object Gzip {
  def decompress(f: java.io.File): Option[String] =
    scala.util.Try {
      val inputStream = new GZIPInputStream(new FileInputStream(f))
      scala.io.Source.fromInputStream(inputStream).mkString
    }.toOption
}