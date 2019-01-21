package com.wcrbrm.blockparser

import io.circe._
import io.circe.generic.semiauto._

case class AbstractPacket(packet: String, time: Long)
case class AmountsPacket(packet: String, time: Long, hash: String, values: List[BigInt])
case class ErrorPacket(packet: String, time: Long, result: String, error: String)

object Packets {
  implicit val abstractPacketDecoder: Decoder[AbstractPacket] = deriveDecoder[AbstractPacket]
  implicit val abstractPacketEncoder: Encoder[AbstractPacket] = deriveEncoder[AbstractPacket]
  implicit val amountsPacketDecoder: Decoder[AmountsPacket] = deriveDecoder[AmountsPacket]
  implicit val amountsPacketEncoder: Encoder[AmountsPacket] = deriveEncoder[AmountsPacket]
  implicit val errorPacketDecoder: Decoder[ErrorPacket] = deriveDecoder[ErrorPacket]
  implicit val errorPacketEncoder: Encoder[ErrorPacket] = deriveEncoder[ErrorPacket]
}
