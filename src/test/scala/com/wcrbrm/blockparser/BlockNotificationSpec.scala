package com.wcrbrm.blockparser

import org.scalatest._
import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

class BlockNotificationSpec extends FlatSpecLike with Matchers {

val newBlock = """
{
    "txIndexes": [
        3187871,
        3187868
    ],
    "nTx": 1,
    "totalBTCSent": 3,
    "estimatedBTCSent": 2,
    "reward": 4,
    "size": 5,
    "blockIndex": 190460,
    "prevBlockIndex": 190457,
    "height": 170359,
    "hash": "00000000000006436073c07dfa188a8fa54fefadf571fd774863cda1b884b90f",
    "mrklRoot": "94e51495e0e8a0c3b78dac1220b2f35ceda8799b0a20cfa68601ed28126cfcc2",
    "version": 1,
    "time": 1331301261,
    "bits": 436942092,
    "nonce": 758889471
}
"""

  it should "be parsed" in {
    import BlockNotificationCodec._
    val decoded = decode[BlockNotification](newBlock)
    decoded.isRight should equal(true)
    decoded.map[String] { 
      case bn: BlockNotification => bn.asJson.noSpaces
    } should not equal("")
  }
}
