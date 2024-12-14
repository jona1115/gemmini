
package gemmini

import chisel3._
import chiseltest._
// import org.scalatest.flatspec.AnyFlatSpec
//import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}
import scala.util.Random.shuffle
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec


//class PipelineTester(c: Pipeline[SInt]) extends ChiselScalatestTester {
class PipelineTester(c: Pipeline[SInt]) extends PeekPokeTester(c) {

  val rnd = new scala.util.Random
  // val rnd: scala.util.Random = scala.util.Random@1f992a3a

  var max_cycles = 100000
  // rnd.setSeed(0L)

  val n_inputs = 100
  val inputs = Seq.fill(n_inputs)(rnd.nextInt())
  // val inputs = (1 to n_inputs).toSeq

  var input_cnt = 0
  var output_cnt = 0

  while (output_cnt < n_inputs && max_cycles > 0) {
    val input_valid = input_cnt < n_inputs && rnd.nextBoolean()
    val output_ready = rnd.nextBoolean()

    poke(c.io.in.valid, input_valid)
    poke(c.io.out.ready, output_ready)

    if (input_cnt < n_inputs) {
      poke(c.io.in.bits, inputs(input_cnt))
    }

    val input_ready = peek(c.io.in.ready) != 0
    val output_valid = peek(c.io.out.valid) != 0

    val input_fire = input_valid && input_ready
    val output_fire = output_valid && output_ready

    if (input_fire) {
      input_cnt += 1
    }

    if (output_fire) {
      assert(inputs(output_cnt) == peek(c.io.out.bits),
        s"Expected: ${inputs(output_cnt)}\nActual: ${peek(c.io.out.bits)}")
      output_cnt += 1
    }

    step(1)
    max_cycles -= 1
  }

  assert(max_cycles > 0, "ran out of max_cycles")
}

// class PipelineUnitTest extends ChiselFlatSpec {

class PipelineUnitTest extends AnyFlatSpec with ChiselScalatestTester {
  
  // NOTE: we could potentially support verilator with this, there appears to be some examples on the chiseltest repo
  // val testerArgs = Array(
  //   "--backend-name", "treadle",
  //   // "--generate-vcd-output", "on",
  //   "--target-dir", "test_run_dir/pipeline"
  // )

  behavior.of("Pipeline")
  it should "work" in {
    test(new Pipeline(SInt(32.W), 10)()).runPeekPoke(new PipelineTester(_))
  }
  it should "work with one element" in {
    test(new Pipeline(SInt(32.W), 1)()).runPeekPoke(new PipelineTester(_))
  } 
  it should "work with no elements" in {
    test(new Pipeline(SInt(32.W), 0)()).runPeekPoke(new PipelineTester(_))
  }

  // it should "work" in {
  //   chisel3.iotesters.Driver.execute(testerArgs, () => new Pipeline(SInt(32.W), 10)()) {
  //     c => new PipelineTester(c)
  //   } should be (true)

  // it should "work with one element" in {
  //   chisel3.iotesters.Driver.execute(testerArgs, () => new Pipeline(SInt(32.W), 1)()) {
  //     c => new PipelineTester(c)
  //   } should be (true)
  // }

  // it should "work with no elements" in {
  //   chisel3.iotesters.Driver.execute(testerArgs, () => new Pipeline(SInt(32.W), 0)()) {
  //     c => new PipelineTester(c)
  //   } should be (true)
  // }
}
