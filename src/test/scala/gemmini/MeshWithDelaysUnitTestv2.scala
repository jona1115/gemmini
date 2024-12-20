
// Commented out in order to run another test briefly
/*
package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MeshWithDelaysUnitTestv2 extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  "MeshWithDelaysUnitTestv2" should "work" in {
    test(new MeshWithDelays(SInt(8.W), SInt(16.W), SInt(32.W), UInt(32.W), Dataflow.WS, /* ... parameters ... */)) { c =>
      // Initialize the DUT
      c.reset.poke(true.B)
      c.clock.step(1)
      c.reset.poke(false.B)

      // Example of poking and peeking signals
      // Make sure these signals exist in your DUT
      c.io.req.valid.poke(true.B)
      // if your DUT changed interface, adjust accordingly
      
      // Step the clock
      c.clock.step(10)

      // Check output
      // c.io.resp.valid.peek() returns a Bool
      // c.io.resp.bits.*.peek() returns a Chisel Literal you can .litValue on

      // etc...
    }
  }
}
*/