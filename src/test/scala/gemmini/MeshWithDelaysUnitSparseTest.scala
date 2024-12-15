package gemmini



import chisel3._
import chiseltest._
import TestUtils._
import scala.util.Random.shuffle

import chisel3.util._
import GemminiISA._
import Util._

import chiseltest.ChiselScalatestTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.iotesters.PeekPokeTester
import chiseltest.simulator.WriteVcdAnnotation

import chisel3.reflect.DataMirror
import chisel3.experimental.requireIsChiselType

// import chiseltest.experimental.TestOptionBuilder._
// import chiseltest.internal.WriteVcdAnnotation

// TODO add test for randomly choosing S
// TODO add test for inputting A, B, and D in different orders
// TODO add test for switching dataflow at runtime
// TODO get a better initialization strategy



class LocalMeshTag extends Bundle with TagQueueTag {

  val testconfig = GemminiCustomConfigs.sparseCPRE581Config
  //val local_addr_t = testconfig.local_addr_t.cloneType
  val block_size = testconfig.meshRows*testconfig.tileRows
  val reservation_station_entries = testconfig.reservation_station_entries // = 16

  val rob_id = UDValid(UInt(log2Up(reservation_station_entries).W))
  val addr = testconfig.local_addr_t.cloneType
  val rows = UInt(log2Up(block_size + 1).W)
  val cols = UInt(log2Up(block_size + 1).W)

  override def make_this_garbage(dummy: Int = 0): Unit = {
    rob_id.valid := false.B
    addr.make_this_garbage()
  }
}

case class MeshTesterInput(A: Matrix[Int], B: Matrix[Int], D: Matrix[Int], flipS: Boolean)


abstract class MeshWithDelaysUnitTest(c: MeshWithDelays[SparseInt, LocalMeshTag], ms: Seq[MeshTesterInput], ts: Seq[MeshTesterInput],
                                      inputGarbageCycles: () => Int, shift: Int = 0,
                                      verbose: Boolean = false)
  extends PeekPokeTester(c)
{
  case class MeshInput(A: Matrix[Int], B: Matrix[Int], D: Matrix[Int], S: Int, M: Int, tag: Int)
  //case class MeshTagInput(A: Matrix[Int], B: Matrix[Int], D: Matrix[Int], S: Int, M: Int, tag: Int)

  case class MeshOutput(C: Matrix[Int], tag: Int)


  def strobeInputs(m: Seq[Int], input: Vec[Vec[SparseInt]], valid: Bool): Unit = {
    poke(valid, true)

    val slices = m.grouped(input.head.length).toList

    for ((slice, i) <- slices.zipWithIndex) {
      for ((elem, j) <- slice.zipWithIndex) {
        poke(input(i)(j).data, elem)
      }
    }
  }

  def pokeAllInputValids(v: Boolean): Unit = {
    val valids = Seq(c.io.a.valid, c.io.b.valid, c.io.d.valid, c.io.req.valid, c.io.req.bits.tag.rob_id.valid) //c.io.s, c.io.tag_in.valid)
    valids.foreach(vpin => poke(vpin, v))
  }

  def allMatrixInputsAreReady(): Boolean = {
    // Ignore m and s here, since they're only supposed to be set once per multiplication
    Seq(c.io.a.ready, c.io.b.ready, c.io.d.ready).forall(r => peek(r) != 0)
  }


  def pokeAddr(data: Int, garbage: Boolean):  Unit = {

    //val local_addr_t = c.io.req.bits.tag.addr.cloneType

    if(garbage)
    {
      poke(c.io.req.bits.tag.addr.accumulate, 1)
      poke(c.io.req.bits.tag.addr.data, 0xFFFFFFFF)
      poke(c.io.req.bits.tag.addr.garbage, 0)
      poke(c.io.req.bits.tag.addr.garbage_bit, 1)
      poke(c.io.req.bits.tag.addr.is_acc_addr, 1)
      poke(c.io.req.bits.tag.addr.norm_cmd, 0)
      poke(c.io.req.bits.tag.addr.read_full_acc_row, 1)
    }
    else
    {
      poke(c.io.req.bits.tag.addr.accumulate, 0)
      poke(c.io.req.bits.tag.addr.data, data)
      poke(c.io.req.bits.tag.addr.garbage, 0)
      poke(c.io.req.bits.tag.addr.garbage_bit, 0)
      poke(c.io.req.bits.tag.addr.is_acc_addr, 0)
      poke(c.io.req.bits.tag.addr.norm_cmd, 0)
      poke(c.io.req.bits.tag.addr.read_full_acc_row, 0)
    }
  }

  assert(ms.head.flipS != 0, "Cannot re-use D for first input")

  /*
  // The matrices must be perfectly sized for this unit test
  assert(ms.forall{ case MeshTesterInput(a, b, d, _) => // case (m1, m2, m3) =>
    rows(d) == c.meshRows * c.tileRows && cols(d) == c.meshColumns * c.tileColumns &&
      rows(d) == cols(d) &&
      dims(a) == dims(d) && dims(b) == dims(d)
  }, "Array must be square and the matrices must be the same size as the array") // TODO get rid of square requirement
  */

  

  val dim = rows(ms.head.D)

  type RawMeshOutputT = Tuple3[Seq[Int], Int, Int]
  var raw_mesh_output = Seq.empty[RawMeshOutputT]

  def updateOutput(): Unit = {
    //val peek_data = peek(c.io.resp.bits.data).map(_.toInt)
    //print(s"OutData: ${peek_data}\n")
    if (peek(c.io.resp.valid) == 1) {
      val peek_c = peek(c.io.resp.bits.data).map(_.toInt) //val peek_c = peek(c.io.out.bits).map(_.toInt)
      // val peek_s = peek(c.io.out_s).map(_.toInt % 2).reduce { (acc, s) =>
      //     assert(acc == s, "s values aren't all the same")
      //     acc
      // }
      val peek_tag = peek(c.io.resp.bits.tag.rob_id.bits).toInt//peek(c.io.tag_out).toInt
      if (peek_tag == 1)
        raw_mesh_output = (peek_c, 0/*peek_s*/, peek_tag) +: raw_mesh_output
    }
  }

  def startup(getOut: Boolean): Unit = {

    // Assert not valid until req.ready
    reset()
    do {
      step(1)
      currentCycle+=1
      poke(c.io.req.valid, 0)
      if (getOut)
        updateOutput()
    } while (peek(c.io.req.ready) == 0 && currentCycle < maxCycles)
    reset()
  }


  def formatMs(ms: Seq[MeshTesterInput]): Seq[MeshInput]
  def formatOut(outs: Seq[Matrix[Int]], tags: Seq[Int]): Seq[MeshOutput]
  def goldResults(ms: Seq[MeshTesterInput]): Seq[Matrix[Int]]

  // Keep from going in an infinite loop 

  val maxCycles = 100 // Set a max cycle limit
  var currentCycle = 0

  val meshRows = 4
  val tileRows = 1


  // True starting point

  startup(false) //wait for ready signal

  poke(c.io.req.bits.flush, 0)

  // Input all matrices
  val meshInputs = formatMs(ms)

  // Simpliest approach to adding SparseInt Tags is to just hold another matrix with them and assign them when data is set
  val sparseTagInputs = formatMs(ts)

  for (meshIn <- meshInputs) {
    
    print(s"Tag: ${meshIn.tag+1}\n")
    print(s"FlipS: ${meshIn.S}\n")
    print("A:\n")
    print2DArray(meshIn.A)
    print("B:\n")
    print2DArray(meshIn.B)
    print("D:\n")
    print2DArray(meshIn.D)
    
    // S is propagate (1 for propagate, which preloads values)
    // M is dataflow (1 for weight stationary)

    // Set Control Bits
    poke(c.io.req.bits.pe_control.propagate, meshIn.S) //poke(c.io.s, meshIn.S)
    poke(c.io.req.bits.pe_control.dataflow, meshIn.M) // poke(c.io.m, meshIn.M)
    poke(c.io.req.bits.pe_control.shift, shift)
    poke(c.io.req.bits.a_transpose, 0)
    poke(c.io.req.bits.bd_transpose, 0)
    poke(c.io.req.bits.total_rows, meshRows*tileRows)


    // Set Tag Bits (Still figuring out)

    pokeAddr(4, false)
    poke(c.io.req.bits.tag.rob_id.valid, 1)
    poke(c.io.req.bits.tag.rob_id.bits, meshIn.tag+1)
    poke(c.io.req.bits.tag.cols, 4)
    poke(c.io.req.bits.tag.rows, 4)

    for ((a, b, d) <- (meshIn.A, meshIn.B, meshIn.D).zipped) {
      
      pokeAllInputValids(true)
      strobeInputs(a, c.io.a.bits, c.io.a.valid)
      strobeInputs(b, c.io.b.bits, c.io.b.valid)
      strobeInputs(d, c.io.d.bits, c.io.d.valid)

      print(s"a: ${a}\n")
      print(s"b: ${b}\n")
      print(s"d: ${d}\n")

      // Garbage cycles turn out to be necessary?
      var garbage_cycles = inputGarbageCycles() + 1

      do {
        step(1)
        currentCycle+=1
        print(s"Input: Current Cycle: ${currentCycle}\n")

        updateOutput()
        pokeAllInputValids(false)
        garbage_cycles -= 1

      } while ((!allMatrixInputsAreReady() || garbage_cycles > 0/*|| peek(c.io.req.ready) == 0*/) && currentCycle < maxCycles) //invalid data while waiting for next ready
    }
  }


  // Flush out the final results

  do {
    //print("Flush Output:\n")
    step(1)
    currentCycle+=1
    print(s"Wait for Resp: Current Cycle: ${currentCycle}\n")

    poke(c.io.req.valid, 0)//poke(c.io.flush.valid, 0)
    updateOutput()
    // Need a way to tell when data is done flushing, the req.bits.last signal sort of works for this, but its still counting the preload as valid output

    //val peek_tag = peek(c.io.resp.bits.tag.rob_id.bits).toInt//peek(c.io.tag_out).toInt

  } while ((peek(c.io.resp.bits.last) == 0 || peek(c.io.resp.bits.tag.rob_id.bits).toInt != 1) && currentCycle < maxCycles)

  // I just like more signals to look at to ensure its not going to keep writing garbage data
  for (i <- 0 until 10) {
    step(1)
    currentCycle+=1
    print(s"Cleanup: Current Cycle: ${currentCycle}\n")
  }


  if (verbose) {
    print("Mesh output:\n")
    print2DArray(raw_mesh_output.map { case (seq, i, j) => seq.map((_, i, j)) })
    print("Mesh output (without tags):\n")
    print2DArray(raw_mesh_output.map { case (seq, i, _) => seq.map((_, i)) })
  }

  // Extract the results from the output
  var output_matrices = Seq(Seq(raw_mesh_output.head._1))
  var output_tags_arrays = Seq(Seq(raw_mesh_output.head._3))
  for (i <- 1 until raw_mesh_output.length) {
    val last_s = raw_mesh_output(i-1)._2
    val (current_c, current_s, current_tag) = raw_mesh_output(i)

    if (current_s == last_s) {
      output_matrices = output_matrices.init :+ (output_matrices.last :+ current_c)
      output_tags_arrays = output_tags_arrays.init :+ (output_tags_arrays.last :+ current_tag)
    } else {
      output_matrices = output_matrices :+ Seq(current_c)
      output_tags_arrays = output_tags_arrays :+ Seq(current_tag)
    }
  }

  // Below is not a 581 note
  // TODO add this back in when tag tests are fixed
  /*assert(output_tags_arrays.forall { ta =>
    ta.takeRight(dim).toSet.size == 1
  }, "output tags do not remain constant when they should")*/

  val output_tags = output_tags_arrays.map(_.last)
  val results = formatOut(output_matrices, output_tags)

  // Get the gold results
  val golds = goldResults(ms)

  // Compare the gold results to the systolic array's outputs
  if (verbose) {
    for ((MeshOutput(out, tag), gold) <- results zip golds) {
      print(s"Tag: $tag\n")
      print("Result:\n")
      print2DArray(out)
      print("Gold:\n")
      print2DArray(gold)
      print("\n")
    }
    for (MeshOutput(out, tag) <- results drop golds.size) {
      print(s"Tag (no result): $tag\n")
      print("Result (no result):\n")
      print2DArray(out)
      print("\n")
    }
    for (gold <- golds drop results.size) {
      print("Gold (no result):\n")
      print2DArray(gold)
      print("\n")
    }
    Console.flush()
  }
  //assert(results.map(_.C) == golds, "Array output is not correct")
  //assert(results.map(_.tag) == meshInputs.init.map(_.tag), "Array tags are not correct")
}

class WSMeshWithDelaysUnitTest(c: MeshWithDelays[SparseInt, LocalMeshTag], ms: Seq[MeshTesterInput], ts: Seq[MeshTesterInput],
                               inputGarbageCyles: () => Int,
                               verbose: Boolean = false)
  extends MeshWithDelaysUnitTest(c, ms, ts, inputGarbageCyles, verbose = verbose) // WS just ignores shift
{
  override def formatMs(ms: Seq[MeshTesterInput]) = {
    // Shift the B matrices down so that they are input at the correct time
    val shifted = (zero(dim), ms.head.B, zero(dim), true) +:
      (ms.tail zip ms).map { case (MeshTesterInput(_, b, _, s), MeshTesterInput(a, _, d, _)) => (a, b, d, s) } :+
      (ms.last.A, zero(dim), ms.last.D, true)

    // Then, reverse B and change the positions of A, B, and D since the IO names are only correct for output-stationary
    val mats = shifted.map{case (a, b, d, s) => (a, d, b.reverse, s)}

    // Finally, add the S and M parameters
    mats.zipWithIndex.map { case ((m1,m2,m3,s),i) => MeshInput(m1, m2, m3, S=s.toInt, M=1, tag=i)}
  }

  override def formatOut(outs: Seq[Matrix[Int]], tags: Seq[Int]) = {
    // TODO
    (outs zip tags).takeRight(ms.length). // Drop initial garbage data from startup
      map(t => (t._1.reverse, t._2)). // Reverse the rows
      reverse.
      map(t => MeshOutput(t._1, t._2))
  }

  override def goldResults(ms: Seq[MeshTesterInput]) = {
    def helper(ms: List[MeshTesterInput], last: Matrix[Int]): List[Matrix[Int]] = ms match {
      case Nil => Nil
      case MeshTesterInput(a, b, d, flipS) :: mstail =>
        val preloaded = if (flipS) b else last
        val new_last = add(d, mult(a, preloaded))
        new_last +: helper(mstail, new_last)
    }

    helper(ms.toList, null)
  }
}

class SparseMeshWithDelaysTester extends AnyFlatSpec with ChiselScalatestTester
{
  val inputType = new SparseInt(16)
  val spatialArrayOutputType = new SparseInt(16)
  val accType = new SparseInt(16)

  val dataflow = Dataflow.WS
  val tree_reduce = false
  val tile_lat = 0
  val output_lat = 1
  val tile_dim = 1
  val mesh_dim = 4
  val shifter_banks = 1

  val meshRows = 4
  val meshColumns = 4

  val tileRows = 1
  val tileColumns = 1

  "SimpleSparseMeshWithDelaysTester" should "work" in {


    test(new MeshWithDelays(inputType, spatialArrayOutputType, accType, (new LocalMeshTag).cloneType, dataflow, tree_reduce, tile_lat, output_lat, tileRows, tileColumns, meshRows, meshColumns, 
    shifter_banks, shifter_banks)).withAnnotations(Seq(WriteVcdAnnotation/*VerilatorBackendAnnotation*/)).runPeekPoke(new WSMeshWithDelaysUnitTest(_, 
    Seq.fill(1)(MeshTesterInput(rand(meshColumns), rand(meshColumns), zero(meshColumns), true)),
    Seq.fill(1)(MeshTesterInput(rand(meshColumns), rand(meshColumns), zero(meshColumns), true)),
    () => 0, verbose = true))
  }
}