package com.wcrbrm.blockparser

import java.io.File
import org.scalatest.{ FlatSpecLike, MustMatchers }

class GzipSpec extends FlatSpecLike with MustMatchers {
    val tempFile = File.createTempFile("testGzip-", "")
    tempFile.deleteOnExit()

    "Gzip" should "compress and decompress into file" in {
        val sampleWord: String = "Hey, brother"
        Gzip.compress(sampleWord.getBytes, tempFile)
        Gzip.decompress(tempFile).get must equal(sampleWord)
    }
}
