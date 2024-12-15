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

class JonathanTestModule/*(w: Int)*/ extends Module {
  val io = IO(new Bundle {
    val in_a = Input(UInt(16.W))
    val in_b = Input(UInt(16.W))
    val out = Output(UInt(16.W))
  })

  io.out := io.in_a + io.in_b
}

class JonathanTest extends AnyFlatSpec with ChiselScalatestTester {
  "JonathanTest 1" should "add very beatifully" in {
    test(new JonathanTestModule())
          .withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.in_a.poke(20.U)
      c.io.in_b.poke(8.U)

      c.io.out.expect(28.U)

      val output = c.io.out.peek()
      val expected = 28.U

      println(s"Output: ${output}\tExpected: ${expected}")
    }
  }

  // "JonathanTest 2" should "fail" in {
  //   test(new JonathanTestModule()) { c => 
  //     c.io.in_a.poke(3.U)
  //     c.io.in_b.poke(8.U)

  //     c.io.out.expect(100.U) // wrong. I want to see how fail looks like

  //     println("Hello") // Donn't print because test fail
  //   }
  // }

  "JonathanTest 3" should "add even very beatifully" in {
    test(new JonathanTestModule())
          .withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.in_a.poke(3.U)
      c.io.in_b.poke(8.U)

      c.io.out.expect(11.U)
    }
  }
}