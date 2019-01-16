package com.wcrbrm.blockparser

import io.circe._
import io.circe.generic.semiauto._

case class ChainHeader(height: Int, hash: String)
object ChainHeader {
  implicit val chainHeaderDecoder: Decoder[ChainHeader] = deriveDecoder[ChainHeader]
  implicit val chainHeaderEncoder: Encoder[ChainHeader] = deriveEncoder[ChainHeader]

  def getAll(sCacheFile: String): List[ChainHeader] = {
    val input = scala.io.Source.fromFile(sCacheFile).getLines.mkString
    parser.decode[List[ChainHeader]](input) match {
      case Right(list) =>
        list
      case Left(error) =>
        println("ERROR:" + error)
        Nil
    }
  }

}
