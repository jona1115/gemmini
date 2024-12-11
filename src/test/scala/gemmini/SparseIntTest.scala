package gemmini

import Arithmetic.SparseIntArithmetic._

import Util._

import chisel3.util._

//import Arithmetic._

import chisel3._
import chiseltest._
import chiseltest.ChiselScalatestTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

// Define a SparseUInt module with an arithmetic selector
class SparseIntModule(w: Int) extends Module {
  val io = IO(new Bundle {
    val in_a = Input(new SparseInt(w))
    val in_b = Input(new SparseInt(w))
    val in_c = Input(new SparseInt(w))
    val in_shift = Input(UInt(5.W))
    val out_a = Output(new SparseInt(w))
    val out_bool = Output(new Bool)
    val opSelect = Input(UInt(4.W))  // Selector to choose arithmetic operation (e.g., 0 for +, 1 for *, etc.)
  })

  // Define the result variable
  val result = Wire(new SparseInt(w))
  val bool_result = Wire(new Bool)

  val a = io.in_a
  val b = io.in_b
  val c = io.in_c
  val shift = io.in_shift

  // Use MuxCase to choose the operation based on opSelect
  result := MuxCase(a, Array(
    (io.opSelect === 0.U) -> (a + b),  // Addition
    (io.opSelect === 1.U) -> (a * b),  // Multiplication
    (io.opSelect === 2.U) -> (a.mac(b, c)),  // MAC (multiply-accumulate)
    (io.opSelect === 3.U) -> (a - b),  // Subtraction
    (io.opSelect === 4.U) -> (a >> shift),  // This is a rounding shift! Rounds away from 0
    (io.opSelect === 5.U) -> (DontCare),  // Outputs a bool
    (io.opSelect === 6.U) -> (a.withWidthOf(b)),  
    (io.opSelect === 7.U) -> (a.clippedToWidthOf(b)),  
    (io.opSelect === 8.U) -> (a.relu),
    (io.opSelect === 9.U) -> (a.zero),
    (io.opSelect === 10.U) -> (a.identity),
    (io.opSelect === 11.U) -> (a.minimum)
  ))

    // Assign the boolean result for opSelect = 5
    bool_result := Mux(io.opSelect === 5.U, (a > b), DontCare)

    // Assign the boolean result to the output
    io.out_bool := bool_result

    io.out_a := result
}

trait SparseIntBehavior {
  this: AnyFlatSpec with ChiselScalatestTester =>

  def testOp(s: Int, op: UInt, a_data: SInt, a_tag: UInt, b_data: SInt, b_tag: UInt, c_data: SInt, c_tag: UInt, shift: UInt): Unit = {
    it should "modify SparseUInt values correctly" in {
      test(new SparseIntModule(s)) { c =>

        println(s"Test: $a_data $b_data $c_data $shift")

        c.io.opSelect.poke(op)
        c.io.in_a.data.poke(a_data)
        c.io.in_a.tags.poke(a_tag)
        c.io.in_b.data.poke(b_data)
        c.io.in_b.tags.poke(b_tag)
        c.io.in_c.data.poke(c_data)
        c.io.in_c.tags.poke(c_tag)
        c.io.in_shift.poke(shift)
        c.clock.step()

        // Get the actual values of the output
        val outData = c.io.out_a.data.peek()
        val outTags = c.io.out_a.tags.peek()

        // Print the actual values (whether expected or not)
        println(s"Output Data: ${outData.litValue}")
        println(s"Output Tags: ${outTags.litValue}")

        c.io.out_a.tags.expect(a_tag)
      }
    }
  }

  def testAddition(s: Int, a_data: SInt, a_tag: UInt, b_data: SInt, b_tag: UInt, c_data: SInt, c_tag: UInt, shift: UInt): Unit = {
    it should "add SparseUInt values correctly" in {
      test(new SparseIntModule(s)) { c =>

        println(s"Addition: $a_data+$b_data")

        c.io.opSelect.poke(0.U)
        c.io.in_a.data.poke(a_data)
        c.io.in_a.tags.poke(a_tag)
        c.io.in_b.data.poke(b_data)
        c.io.in_b.tags.poke(b_tag)
        c.io.in_c.data.poke(c_data)
        c.io.in_c.tags.poke(c_tag)
        c.io.in_shift.poke(shift)
        c.clock.step()

        // Get the actual values of the output
        val outData = c.io.out_a.data.peek()
        val outTags = c.io.out_a.tags.peek()

        // Print the actual values (whether expected or not)
        println(s"Output Data: ${outData.litValue}")
        println(s"Output Tags: ${outTags.litValue}")

        c.io.out_a.tags.expect(a_tag)
      }
    }
  }

  def testMultiplication(s: Int, a_data: SInt, a_tag: UInt, b_data: SInt, b_tag: UInt, c_data: SInt, c_tag: UInt, shift: UInt): Unit = {
    it should "multiply SparseUInt values correctly" in {
      test(new SparseIntModule(s)) { c =>

        println(s"Multiplication: $a_data+$b_data")

        c.io.opSelect.poke(1.U)
        c.io.in_a.data.poke(a_data)
        c.io.in_a.tags.poke(a_tag)
        c.io.in_b.data.poke(b_data)
        c.io.in_b.tags.poke(b_tag)
        c.io.in_c.data.poke(c_data)
        c.io.in_c.tags.poke(c_tag)
        c.io.in_shift.poke(shift)
        c.clock.step()

        // Get the actual values of the output
        val outData = c.io.out_a.data.peek()
        val outTags = c.io.out_a.tags.peek()

        // Print the actual values (whether expected or not)
        println(s"Output Data: ${outData.litValue}")
        println(s"Output Tags: ${outTags.litValue}")

        c.io.out_a.tags.expect(a_tag)
      }
    }
  }

  def testMac(s: Int, a_data: SInt, a_tag: UInt, b_data: SInt, b_tag: UInt, c_data: SInt, c_tag: UInt, shift: UInt): Unit = {
    it should "mac SparseUInt values correctly" in {
      test(new SparseIntModule(s)) { c =>

        println(s"Multiplication: $a_data+$b_data")

        c.io.opSelect.poke(2.U)
        c.io.in_a.data.poke(a_data)
        c.io.in_a.tags.poke(a_tag)
        c.io.in_b.data.poke(b_data)
        c.io.in_b.tags.poke(b_tag)
        c.io.in_c.data.poke(c_data)
        c.io.in_c.tags.poke(c_tag)
        c.io.in_shift.poke(shift)
        c.clock.step()

        // Get the actual values of the output
        val outData = c.io.out_a.data.peek()
        val outTags = c.io.out_a.tags.peek()

        // Print the actual values (whether expected or not)
        println(s"Output Data: ${outData.litValue}")
        println(s"Output Tags: ${outTags.litValue}")

        c.io.out_a.tags.expect(a_tag)
      }
    }
  }

  def testSubtraction(s: Int, a_data: SInt, a_tag: UInt, b_data: SInt, b_tag: UInt, c_data: SInt, c_tag: UInt, shift: UInt): Unit = {
    it should "subtract SparseUInt values correctly" in {
      test(new SparseIntModule(s)) { c =>

        println(s"Subtraction: $a_data+$b_data")

        c.io.opSelect.poke(3.U)
        c.io.in_a.data.poke(a_data)
        c.io.in_a.tags.poke(a_tag)
        c.io.in_b.data.poke(b_data)
        c.io.in_b.tags.poke(b_tag)
        c.io.in_c.data.poke(c_data)
        c.io.in_c.tags.poke(c_tag)
        c.io.in_shift.poke(shift)
        c.clock.step()

        // Get the actual values of the output
        val outData = c.io.out_a.data.peek()
        val outTags = c.io.out_a.tags.peek()

        // Print the actual values (whether expected or not)
        println(s"Output Data: ${outData.litValue}")
        println(s"Output Tags: ${outTags.litValue}")

        c.io.out_a.tags.expect(a_tag)
      }
    }
  }

  //....

}

// This test case is written in a style that is finely granular, with one test case per operation and input-output combination.
// There currently isn't consensus on a recommended test granularity, but factors to consider include:
// - granularity of test failures
// - number of test cases reported
class SparseIntTest extends AnyFlatSpec with SparseIntBehavior with ChiselScalatestTester with Matchers {
  behavior.of("SparseInt")
  val w = 16
  println("Sparse Int Tests")

  (it should behave).like(testAddition(w, 1.S, 1.U, 2.S, 2.U, 3.S, 3.U, 0.U))

  (it should behave).like(testOp(w, 0.U, 1.S, 1.U, 2.S, 2.U, 3.S, 3.U, 0.U))
  // val testData: List[(SparseInt, SparseInt, SparseInt, UInt)] = List[(SparseInt, SparseInt, SparseInt, UInt)](
  //   (SparseInt(w, 1.S, 1.U), SparseInt(w, 1.S, 2.U), SparseInt(w, 0.S, 0.U), 0.U),
  //   (SparseInt(w, 2.S, 1.U), SparseInt(w, 1.S, 2.U), SparseInt(w, 0.S, 0.U), 0.U),
  //   (SparseInt(w, 1.S, 1.U), SparseInt(w, 2.S, 2.U), SparseInt(w, 0.S, 0.U), 0.U)
  // )
  // testData.foreach { data =>
  //   // TODO: re-use a single DUT elaboration / compilation, once https://github.com/ucb-bar/chisel-testers2/issues/212 is resolved
  //   (it should behave).like(testAddition(w, data._1, data._2, data._3, data._4))
  //   // (it should behave).like(testSubtraction(data._1, data._2, 16))
  //   // (it should behave).like(testOr(data._1, data._2, 16))
  //   // (it should behave).like(testAnd(data._1, data._2, 16))
  // }
}
