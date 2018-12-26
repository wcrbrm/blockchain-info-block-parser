package com.wcrbrm.blockparser

import okhttp3._
import io.circe._

trait ConvertedToJson {
  def toJson: String
}
case class Op(value: String) extends ConvertedToJson {
  def toJson: String = Json.fromFields(List(("op", Json.fromString(value)))).noSpaces
}
object OpPing extends Op("ping")
object OpBlocksSub extends Op("blocks_sub")
object OpBlocksUnsub extends Op("blocks_unsub")
object OpPingBlock extends Op("ping_block")
object OpPingTx extends Op("ping_tx")

object BlockchainInfoListener extends WebSocketListener {

  def deliver(webSocket: WebSocket, packet: ConvertedToJson) {
    val str: String = packet.toJson
    println("sending " + str)
    webSocket.send(str)
  }

  override def onOpen(webSocket: WebSocket, response: Response) {
    println("WebSocket connected")
    println("Sending ping message")
    deliver(webSocket, OpPing)
    deliver(webSocket, OpBlocksSub)
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
