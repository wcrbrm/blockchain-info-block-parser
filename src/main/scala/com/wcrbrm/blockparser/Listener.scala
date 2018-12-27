package com.wcrbrm.blockparser

import okhttp3._
import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

trait JsonMessage {
  def asJson: String
}
case class Op(value: String) extends JsonMessage {
  def asJson: String = Json.fromFields(List(("op", Json.fromString(value)))).noSpaces
}
object OpPing extends Op("ping")
object OpBlocksSub extends Op("blocks_sub")
object OpBlocksUnsub extends Op("blocks_unsub")
object OpPingBlock extends Op("ping_block")
object OpPingTx extends Op("ping_tx")

case class BlockFoundBy(
  description: Option[String],
  ip: Option[String],
  link: Option[String],
  time: Option[Long]
)

case class BlockNotification (
  txIndexes: List[Long],  // list of transaction indexes
  nTx: Option[Long],
  totalBTCSent: BigInt,
  estimatedBTCSent: BigInt,
  reward: Option[BigInt],
  size: Option[Long],
  weight: Option[Long],
  blockIndex: Long,
  prevBlockIndex: Long,
  height: Long,
  hash: String,
  mrklRoot: Option[String],
  version: Option[Long],
  time: Long,
  bits: Option[Long],
  nonce: Option[Long],
  foundBy: Option[BlockFoundBy]
)

object BlockFoundByCodec {
  implicit val encodeFoundBy: Encoder[BlockFoundBy] =
    Encoder.forProduct4(
        "description", "ip", "link", "time"
    )(f => (f.description, f.ip, f.link, f.time))
  implicit val decodeFoundBy: Decoder[BlockFoundBy] =
    Decoder.forProduct4(
     "description", "ip", "link", "time"
    )(BlockFoundBy.apply)
}

object BlockNotificationCodec {
  import BlockFoundByCodec._
  implicit val encodeBlockNotification: Encoder[BlockNotification] =
    Encoder.forProduct17(
      "txIndexes", "nTx", "totalBTCSent", "estimatedBTCSent", "reward", 
      "size", "weight", "blockIndex", "prevBlockIndex", "height", 
      "hash", "mrklRoot", "version", "time", "bits", 
      "nonce", "foundBy"
    )(b => (b.txIndexes, b.nTx, b.totalBTCSent, b.estimatedBTCSent, b.reward, 
      b.size, b.weight, b.blockIndex, b.prevBlockIndex, b.height, 
      b.hash, b.mrklRoot, b.version, b.time, b.bits, 
      b.nonce, b.foundBy))
  implicit val decodeBlockNotification: Decoder[BlockNotification] =
    Decoder.forProduct17(
      "txIndexes", "nTx", "totalBTCSent", "estimatedBTCSent", "reward", 
      "size", "weight", "blockIndex", "prevBlockIndex", "height", 
      "hash", "mrklRoot", "version", "time", "bits", 
      "nonce", "foundBy"
    )(BlockNotification.apply)
}

object BlockchainInfoListener extends WebSocketListener {
  def fire(webSocket: WebSocket, packet: JsonMessage) {
    val str: String = packet.asJson
    println("sending " + str)
    webSocket.send(str)
  }
  override def onOpen(webSocket: WebSocket, response: Response) {
    println("WebSocket connected")
    println("Sending ping message")
    fire(webSocket, OpPing)
    fire(webSocket, OpBlocksSub)
  }
  override def onMessage(webSocket: WebSocket, text: String) {
    println("Receiving : " + text)
  }
  override def onClosing(webSocket: WebSocket, code: Int, reason: String) {
    println("Closing code="+ code + " reason=" + reason )
  }
  override def onFailure(webSocket: WebSocket, t: Throwable, response: Response) {
    println("Error : " + t.getMessage)
  }
}

object Listener extends App {
  val wsUrl = "wss://ws.blockchain.info/inv"
  val client = new OkHttpClient
  val request = new Request.Builder().url(wsUrl).build
  val ws = client.newWebSocket(request, BlockchainInfoListener)
}
