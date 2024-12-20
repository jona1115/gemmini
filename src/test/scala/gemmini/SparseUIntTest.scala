/*
package gemmini

import Arithmetic.SparseUIntArithmetic._

import Util._

import chisel3.util._

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec  // Use AnyFlatSpec for testing

// Written in part with help from chatgpt

// Define a SparseUInt module with an arithmetic selector
class SparseUIntModule(dataWidth: Int, tagsWidth: Int) extends Module {
  val io = IO(new Bundle {
    val in_a = Input(new SparseUInt(dataWidth, tagsWidth))
    val in_b = Input(new SparseUInt(dataWidth, tagsWidth))
    val in_c = Input(new SparseUInt(dataWidth, tagsWidth))
    val in_shift = Input(UInt(5.W))
    val out_a = Output(new SparseUInt(dataWidth, tagsWidth))
    val out_bool = Output(new Bool)
    val opSelect = Input(UInt(4.W))  // Selector to choose arithmetic operation (e.g., 0 for +, 1 for *, etc.)
  })

  // Define the result variable
  val result = Wire(new SparseUInt(dataWidth, tagsWidth))
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

    //   // Assign the boolean result for opSelect = 5
    bool_result := Mux(io.opSelect === 5.U, (a > b), DontCare)

    // // Assign the boolean result to the output
    io.out_bool := bool_result

    io.out_a := result
}

class SparseUIntTest extends AnyFlatSpec with ChiselScalatestTester {

  "SparseUInt ArithmeticOps" should "add SparseUInt values correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>

      // Poke values into the inputs
      c.io.in_a.data.poke(5.U)  // Assign 5 to data of in1
      c.io.in_a.tags.poke(1.U)  // Assign 1 to tags of in1

      c.io.in_b.data.poke(3.U)  // Assign 3 to data of in2
      c.io.in_b.tags.poke(2.U)  // Assign 2 to tags of in2

      c.io.opSelect.poke(0.U)

      // Wait for the operation to complete (1 cycle)
      c.clock.step(1)

      // Get the actual values of the output
      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      // Print the actual values (whether expected or not)
      println("Addition: 5+3")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(8.U) 
      c.io.out_a.tags.expect(1.U)

    }
  }

  "SparseUInt ArithmeticOps" should "multiply SparseUInt values correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(5.U)  // Assign 5 to data of in1
      c.io.in_a.tags.poke(1.U)  // Assign 1 to tags of in1

      c.io.in_b.data.poke(3.U)  // Assign 3 to data of in2
      c.io.in_b.tags.poke(2.U)  // Assign 2 to tags of in2

      // Select the operation: 1 for multiplication
      c.io.opSelect.poke(1.U)

      // Wait for the operation to complete
      c.clock.step(1)

      // Get the actual values of the output
      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      // Print the actual values (whether expected or not)
      println("Multiplication: 5*3")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      // Check the expected result (5 * 3 = 15, tags: 1 | 2 = 3)
      c.io.out_a.data.expect(15.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply MAC operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(5.U)  // Assign 5 to data of in1
      c.io.in_a.tags.poke(1.U)  // Assign 1 to tags of in1

      c.io.in_b.data.poke(3.U)  // Assign 3 to data of in2
      c.io.in_b.tags.poke(2.U)  // Assign 2 to tags of in2

      c.io.in_c.data.poke(4.U)  // Assign 3 to data of in2
      c.io.in_c.tags.poke(3.U)  // Assign 2 to tags of in2

      // Select the operation: 2 for MAC (Multiply and accumulate)
      c.io.opSelect.poke(2.U)

      // Wait for the operation to complete
      c.clock.step(1)

      // Get the actual values of the output
      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      // Print the actual values (whether expected or not)
      println("MAC: 5+4*3")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      // For MAC, 5 * 3 + 5 = 20, tags: 1 | 2 = 3
      c.io.out_a.data.expect(17.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply subtration operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(5.U)
      c.io.in_a.tags.poke(1.U)

      c.io.in_b.data.poke(3.U)
      c.io.in_b.tags.poke(2.U)

      c.io.opSelect.poke(3.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println("Subtract: 5-3")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(2.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply rounding shift operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      // c.io.in_b.data.poke(1.U)
      // c.io.in_b.tags.poke(2.U) 

      c.io.in_shift.poke(1.U)

      c.io.opSelect.poke(4.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println(">>: 4>>1")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(8.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply > operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      c.io.in_b.data.poke(1.U)
      c.io.in_b.tags.poke(2.U) 


      c.io.opSelect.poke(5.U)

      c.clock.step(1)

      val outData = c.io.out_bool.peek()
      //val outTags = c.io.out_a.tags.peek()

      println(">: 4>2")
      println(s"Output Data: ${outData.litValue}")
      //println(s"Output Tags: ${outTags.litValue}")

      c.io.out_bool.expect(1.U)
      //c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply withWidthOf operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      c.io.in_b.data.poke(1.U)
      c.io.in_b.tags.poke(2.U) 

      c.io.opSelect.poke(6.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println("WithWidthOf: 8,8")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(4.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply clippedToWidthOf operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      c.io.in_b.data.poke(1.U)
      c.io.in_b.tags.poke(2.U) 

      c.io.opSelect.poke(7.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println("clippedToWidthOf: 8,8")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(4.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply relu operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      c.io.opSelect.poke(8.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println("relu:")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(4.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply zero operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      c.io.opSelect.poke(9.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println("zero:")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(0.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply identity operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      c.io.opSelect.poke(10.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println("identity:")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(1.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

  "SparseUInt ArithmeticOps" should "apply minimum operation correctly" in {
    test(new SparseUIntModule(8, 8)) { c =>
      // Poke values into the inputs
      c.io.in_a.data.poke(4.U)
      c.io.in_a.tags.poke(1.U)

      c.io.opSelect.poke(11.U)

      c.clock.step(1)

      val outData = c.io.out_a.data.peek()
      val outTags = c.io.out_a.tags.peek()

      println("minimum:")
      println(s"Output Data: ${outData.litValue}")
      println(s"Output Tags: ${outTags.litValue}")

      c.io.out_a.data.expect(0.U)
      c.io.out_a.tags.expect(1.U)
    }
  }

}

*/
