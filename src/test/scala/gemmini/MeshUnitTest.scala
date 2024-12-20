
package gemmini

import Arithmetic.SparseIntArithmetic._

import Util._

import chisel3.util._

import TestUtils._

import chisel3._
import chiseltest._
import chiseltest.ChiselScalatestTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chiseltest.iotesters.PeekPokeTester

/*
case class MeshTesterInput(A: Matrix[Int], B: Matrix[Int], D: Matrix[Int], flipS: Boolean)

abstract class MeshUnitTest(c: Mesh[SInt], ms: Seq[MeshTesterInput], inputGarbageCycles: () => Int, shift: Int = 0, verbose: Boolean = false)

  extends PeekPokeTester(c)
  //extends AnyFlatSpec with ChiselScalatestTester
{
  case class MeshInput(A: Matrix[Int], B: Matrix[Int], D: Matrix[Int], S: Int, M: Int, tag: Int)
  case class MeshOutput(C: Matrix[Int], tag: Int)

  def strobeInputs[T <: Bits](m: Seq[Int], input: Vec[Vec[T]]): Unit = {
    val slices = m.grouped(input.head.length).toList

    for ((slice, i) <- slices.zipWithIndex) {
      for ((elem, j) <- slice.zipWithIndex) {
        poke(input(i)(j),(elem))
      }
    }
  }

  def pokeAllInputValids(v: Boolean): Unit = {
    for (i <- 0 until c.meshColumns) {
        for (j <- 0 until c.tileColumns) {
            poke(c.io.in_valid(i)(j), v.B)
        }
    }
      
  }

  def pokeAllInputs[T <: Bits](value: Int, input: Vec[Vec[T]]): Unit = {
    for (i <- 0 until c.meshColumns) {
        for (j <- 0 until c.tileColumns) {
            poke(input(i)(j), value.U)
        }
    }
  }

  def pokeAllControl(value: Int, input: Vec[Vec[PEControl[SInt]]]): Unit = {
    for (i <- 0 until c.meshColumns) {
        for (j <- 0 until c.tileColumns) {
            poke(input(i)(j).propagate, value.U)
            poke(input(i)(j).shift, shift.U)
            poke(input(i)(j).dataflow, 1.U)
        }
    }

  }

//   def allMatrixInputsAreReady(): Boolean = {
//     // Ignore m and s here, since they're only supposed to be set once per multiplication
//     Seq(c.io.in_a.ready, c.io.in_b.ready, c.io.in_d.ready).forall(r => peek(r) != 0)
//   }

//   assert(ms.head.flipS != 0, "Cannot re-use D for first input")

//   // The matrices must be perfectly sized for this unit test
//   assert(ms.forall{ case MeshTesterInput(a, b, d, _) => // case (m1, m2, m3) =>
//     rows(d) == c.meshRows * c.tileRows && cols(d) == c.meshColumns * c.tileColumns &&
//       rows(d) == cols(d) &&
//       dims(a) == dims(d) && dims(b) == dims(d)
//   }, "Array must be square and the matrices must be the same size as the array") // TODO get rid of square requirement

  val dim = rows(ms.head.D)

  type RawMeshOutputT = Tuple3[Seq[Int], Int, Int]
  var raw_mesh_output = Seq.empty[RawMeshOutputT]

//   def updateOutput(): Unit = {
//     if (peek(c.io.out.valid) == 1) {
//       val peek_c = peek(c.io.out.bits).map(_.toInt)
//       val peek_s = peek(c.io.out_s).map(_.toInt % 2).reduce { (acc, s) =>
//           assert(acc == s, "s values aren't all the same")
//           acc
//       }
//       val peek_tag = peek(c.io.tag_out).toInt
//       if (peek_tag != -1)
//         raw_mesh_output = (peek_c, peek_s, peek_tag) +: raw_mesh_output
//     }
//   }

  def updateOutput(): Unit = {
    if (peek(c.io.out_valid(0)(0)) == 1) {
        //Out B, C and Valid
      val peek_b = peek(c.io.out_b).map(_.toInt)
      //val peek_c = peek(c.io.out_c).map(_.toInt)
      val peek_s = peek(c.io.out_control(0)(0).propagate).toInt

      val peek_tag = peek(c.io.out_id(0)(0)).toInt
      if (peek_tag != 0){
        raw_mesh_output = (peek_b, peek_s, peek_tag) +: raw_mesh_output
      }
    }
  }

  def startup(getOut: Boolean): Unit = {
    //poke(c.io.tag, -1)
    pokeAllInputValids(true)
    pokeAllInputs(0,c.io.in_id)
    //poke(c.io.shift, shift)
    //poke(c.io.flush.bits, 2)
    reset()
    //poke(c.io.flush.valid, 1)
    //do {
    for (i <- 0 until 8) {
      step(1)
      //poke(c.io.flush.valid, 0)
      if (getOut)
        updateOutput()
    } //while (peek(c.io.flush.ready) == 0)
    reset()
  }

def shiftMatrix[T <: Bits](matrix: Seq[Seq[Int]], numRows: Int): Seq[Seq[Int]] = {
  // Initialize an empty result matrix with the desired number of rows and columns
  val resultMatrix = Array.fill(numRows)(Array.fill(matrix.head.length)(0))  // Matrix of 0s
  
  // Fill the matrix progressively
  for (i <- matrix.indices) {
    val row = matrix(i)

    // Shift the values to the correct positions in the result matrix
    for (j <- 0 until row.length) {
      if (i + j < numRows) {  // Ensure we're within the bounds of the expanded matrix
        resultMatrix(i + j)(j) = row(j)
      }
    }
  }

  // Convert the result matrix to Seq[Seq] and return
  resultMatrix.map(_.toSeq)
}

  def formatMs(ms: Seq[MeshTesterInput]): Seq[MeshInput]
  def formatOut(outs: Seq[Matrix[Int]], tags: Seq[Int]): Seq[MeshOutput]
  def goldResults(ms: Seq[MeshTesterInput]): Seq[Matrix[Int]]

  val maxCycles = 1000 // Set a max cycle limit
  var currentCycle = 0
  var propagate = 0  // Initialize to 0
  startup(false)

  // Input all matrices
  val meshInputs = formatMs(ms)
  for (meshIn <- meshInputs) {


    if (meshIn.S == 1) {
      propagate = if (propagate == 0) 1 else 0
    }

    print(s"Tag: ${meshIn.tag}\n")
    print(s"FlipS: ${meshIn.S}\n")
    print(s"Propagate: ${propagate}\n")
    print("A:\n")
    print2DArray(meshIn.A)
    print("B:\n")
    print2DArray(meshIn.B)
    print("D:\n")
    print2DArray(meshIn.D)
    
    pokeAllInputs(meshIn.tag, c.io.in_id)
    pokeAllControl(propagate, c.io.in_control)

    // Not sure what this is? always 1
    //c.io.m.(meshIn.M)

    //ID of MatMul
    //poke(c.io.in_id,meshIn.tag)


    val shiftedA = shiftMatrix(meshIn.A, 7)
    print("Shifted A:\n")
    print2DArray(shiftedA)

    // Determine the maximum number of rows (in case A, B, and D have different row counts)
    val maxRows = if (propagate == 0) shiftedA.length else c.meshRows
    

    // Loop for the maximum number of rows
    for (i <- 0 until maxRows) {
      // Ensure we don't go out of bounds for meshIn.B and meshIn.D
      val a = shiftedA(i)
      val b = meshIn.B.lift(i).getOrElse(Seq.fill(a.length)(0))  // Default to zeroed row if B doesn't have that row
      val d = meshIn.D.lift(i).getOrElse(Seq.fill(a.length)(0))  // Default to zeroed row if D doesn't have that row

      // Debugging output
      print(s"a: ${a}\n")
      print(s"b: ${b}\n")
      print(s"d: ${d}\n")
      print(s"Cycle Count: ${currentCycle}\n")

      pokeAllInputValids(true)
      strobeInputs(a, c.io.in_a)
      strobeInputs(b, c.io.in_b)
      strobeInputs(d, c.io.in_d)

      var garbage_cycles = inputGarbageCycles() + 1

      // Feed in garbage data
    

    // for ((a, b, d) <- (shiftedA, meshIn.B, meshIn.D).zipped) {

    //   pokeAllInputValids(true)
    //   //pokeAllInputs(true, c.io.in_id)

    //   print(s"a: ${a}\n")
    //   print(s"b: ${b}\n")
    //   print(s"d: ${d}\n")
    //   print(s"Cycle Count: ${currentCycle}\n")

    //   strobeInputs(a, c.io.in_a)
    //   strobeInputs(b, c.io.in_b)
    //   strobeInputs(d, c.io.in_d)

    //   var garbage_cycles = inputGarbageCycles() + 1

      // Feed in garbage data
      do {
        step(1)
        currentCycle +=1
        //c.clock.step()
        updateOutput()

        // Put in garbage data
        pokeAllInputValids(false)
        garbage_cycles -= 1

      } while (garbage_cycles > 0)
    }
  }

  print(s"Cycle Count: ${currentCycle}\n")
  // Flush out the final results
  //poke(c.io.s, 1)
  pokeAllControl(1, c.io.in_control)
  // poke(c.io.flush.valid, 1)
  poke(c.io.out_last(0)(0),1.U)
   
  // do {
  for (i <- 0 until 8) {
    step(1)
    currentCycle+=1
    //poke(c.io.flush.valid, 0)
    updateOutput()
  }

  print(s"Cycle Count: ${currentCycle}\n")
//  } while (peek(c.io.flush.ready) == 0)
  // } while (peek(c.io.out_last(0)(0)) == 0)

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

  assert(results.map(_.C) == golds, "Array output is not correct")
  //assert(results.map(_.tag) == meshInputs.init.map(_.tag), "Array tags are not correct")
}


class WSMeshUnitTest(c: Mesh[SInt], ms: Seq[MeshTesterInput], inputGarbageCyles: () => Int, verbose: Boolean = false)
  extends MeshUnitTest(c, ms, inputGarbageCyles, verbose = verbose) // WS just ignores shift
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


class MeshTester extends AnyFlatSpec with ChiselScalatestTester //extends ChiselFlatSpec
{

  "SimpleMeshWithDelaysTester" should "work" in {
    // This is really just here to help with debugging
    val dataflow = Dataflow.WS
    val tree_reduce = false
    val tile_lat = 1
    val max_matmuls = 1
    val output_lat = 1
    val tile_dim = 1
    val mesh_dim = 4

//new WSMeshWithDelaysUnitTest(c, Seq.fill(1)(MeshTesterInput(rand(dim), rand(dim), rand(dim), true)), () => 0, verbose = true)

// UNCOMMENT TO TEST (beware of infinite loop XD)

    test(new Mesh(SInt(16.W), SInt(16.W), SInt(16.W), dataflow, tree_reduce, tile_lat, max_matmuls, output_lat, tile_dim, tile_dim, mesh_dim, mesh_dim)).runPeekPoke(
        new WSMeshUnitTest(_, Seq.fill(1)(MeshTesterInput(rand(mesh_dim), identity(mesh_dim), zero(mesh_dim), true)), () => 0, verbose = true))

    }
  }

//     test(
//         () => new Mesh(SInt(16.W), SInt(16.W), SInt(16.W), dataflow, tree_reduce, tile_lat, max_matmuls, output_lat, tile_dim, tile_dim, mesh_dim, mesh_dim)) { 
//         c => new WSMeshUnitTest(c, Seq.fill(1)(MeshTesterInput(rand(dim), rand(dim), rand(dim), true)), verbose = true)
//     } should be(true)
//    }


//     iotesters.Driver.execute(Array("--backend-name", "treadle", "--generate-vcd-output", "on"),
//       () => new MeshWithDelays(SInt(8.W), SInt(16.W), SInt(32.W), UInt(32.W), Dataflow.WS, 1, 1, dim, dim,1, 1)) {
//         // c => new OSMeshWithDelaysUnitTest(c, Seq.fill(1)(MeshTesterInput(rand(dim), rand(dim), rand(dim), true)), () => 0, shift = 0, verbose = true)
//         c => new WSMeshWithDelaysUnitTest(c, Seq.fill(1)(MeshTesterInput(rand(dim), rand(dim), rand(dim), true)), () => 0, verbose = true)
//     } should be(true)
//   }

//   // Fully pipelined
//   it should "work fully pipelined with no delays" in {
//     for (df <- dataflow_testers) {
//       iotesters.Driver.execute(Array("--backend-name", "treadle"),
//         () => new MeshWithDelays(SInt(8.W), SInt(16.W), SInt(32.W), UInt(32.W), Dataflow.BOTH, 1, 1, 8, 8, 1, 1)) {
//         c => df(c, Seq.fill(8)(MeshTesterInput(rand(8), rand(8), rand(8), true)), () => 0, 0)
//       } should be(true)
//     }
//   }


*/
