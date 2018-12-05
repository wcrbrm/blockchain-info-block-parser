package com.wcrbrm.blockparser

import akka.actor.{ Props, ActorSystem }
import akka.pattern.ask
import scala.concurrent.duration._
import akka.testkit.{ TestKit, TestProbe }
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, MustMatchers }

class JsonReaderSpec extends TestKit(ActorSystem("test-system")) 
    with FlatSpecLike
    with BeforeAndAfterAll
    with MustMatchers {

    override def afterAll = {
        TestKit.shutdownActorSystem(system)
    }

    val validBlockExample: String = """
    { "blocks" : [{
        "hash": "0000000000000000001a7687663a2e1548ee1fbeff63f99a565c9f189a870d01", 
        "prev_block": "00000000000000000011a978a37d1e01772b48f201ffabc53507b444834632a8", 
        "height": 544301, 
        "time": 1538628806,
        "tx": [
            { "out": [ { "value": 1 }, { "value": 2 } ] },
            { "out": [ { "value": 3 }, { "value": 4 } ] },
            { "out": [ { "value": 3 }, { "value": 4 } ] }
        ]
    }]}
    """

    it should "parse simple valid block directly" in {
        val block = JsonBlock(validBlockExample)
        block.height must equal(544301)
        block.amounts.size must equal(2)
        block.amounts(4) must equal(2)
        block.amounts(3) must equal(2)
    }

    it should "send proper amounts spectre back from actor" in {
        val probe = TestProbe()
        val actor = system.actorOf(JsonReaderActor.props("12345"), "block-12345")
        within(1.second) {
            probe.send(actor, JsonReaderActor.ParseJson(validBlockExample))
            val spectre: Spectre = probe.expectMsgType[Spectre]
            spectre.height must equal(544301)
            spectre.amounts.size must equal(2)
            spectre.amounts(4) must equal(2)
            spectre.amounts(3) must equal(2)
        }
    }

}