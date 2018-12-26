package com.wcrbrm.blockparser

import okhttp3._
import io.circe._

case class Op(value: String) {
  def toJson: String = Json.fromFields(List(("op", Json.fromString(value)))).noSpaces
}
object OpPing extends Op("ping")
object OpBlocksSub extends Op("blocks_sub")
object OpBlocksUnsub extends Op("blocks_unsub")
object OpPingBlock extends Op("ping_block")
object OpPingTx extends Op("ping_tx")

object BlockchainInfoListener extends WebSocketListener {
  override def onOpen(webSocket: WebSocket, response: Response) {
    println("WebSocket connected")
    println("Sending ping message")

    println("(sending) " + OpPing.toJson)
    webSocket.send(OpPing.toJson)
    println("(sending) " + OpBlocksSub.toJson)
    webSocket.send(OpBlocksSub.toJson)
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
