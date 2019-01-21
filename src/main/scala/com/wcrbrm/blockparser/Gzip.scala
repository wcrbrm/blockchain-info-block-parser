package com.wcrbrm.blockparser

import java.io.{ File, FileOutputStream, FileInputStream }
import java.util.zip.{ GZIPInputStream, GZIPOutputStream }

object Gzip {

  def compress(input: Array[Byte], fOut: File): Unit = {
    val gzip = new GZIPOutputStream(new FileOutputStream(fOut))
    gzip.write(input)
    gzip.close()
  }

  def decompress(fIn: File): Option[String] =
    scala.util.Try {
      val inputStream = new GZIPInputStream(new FileInputStream(fIn))
      scala.io.Source.fromInputStream(inputStream).mkString
    }.toOption
}