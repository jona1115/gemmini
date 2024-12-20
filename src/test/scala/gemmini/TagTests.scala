package gemmini

import Arithmetic.SparseIntArithmetic._

import Util._

import chisel3.util._


import chisel3._
import chiseltest._
import chiseltest.ChiselScalatestTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

// Define a SparseUInt module with an arithmetic selector
class SparseIntTagModule(w: Int) extends Module {
  val io = IO(new Bundle {
    val in_a = Input(new SparseInt(w))
    val out_tag = Output(UInt(w.W))
    val out_forward = Output(new Bool)
    val out_row = Output(UInt(w.W))
  })

    // Define the result variable
    io.out_tag := io.in_a.tags
    io.out_forward := io.in_a.forward
    io.out_row := io.in_a.row
}

class TagTests extends AnyFlatSpec with ChiselScalatestTester {

  "SparseInt Tag" should "recieve values correctly" in {
    test(new SparseIntTagModule(8)) { c =>

      val tag = 0xE0.U

      // Poke values into the inputs
      c.io.in_a.data.poke(5.S)  // Assign 5 to data of in1
      c.io.in_a.tags.poke(tag)  // Assign 1 to tags of in1

      // Wait for the operation to complete (1 cycle)
      c.clock.step(1)

      // Get the actual values of the output
      val outTags = c.io.out_tag.peek()
      val outForward = c.io.out_forward.peek()
      val outRow = c.io.out_row.peek()

      // Print the actual values (whether expected or not)
      println("Addition: 5+3")
      println(s"Output Tag: ${outTags.litValue}")
      println(s"Output Forwards: ${outForward.litValue}")
      println(s"Output Row: ${outRow.litValue}")
      

      c.io.out_tag.expect(tag)
      c.io.out_forward.expect(1.U) 

    }
  }
}