package gemmini

import org.chipsalliance.cde.config.{Config, Parameters}
import chisel3._
import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.SystemBusKey
//import freechips.rocketchip.tile.BuildRoCC

import freechips.rocketchip.diplomacy.{LazyModule, ValName}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tile.{BuildRoCC, OpcodeSet}


import Arithmetic.SparseIntArithmetic._


object GemminiCustomConfigs {
  // Default configurations
  val defaultConfig = GemminiConfigs.defaultConfig
  val defaultFpConfig = GemminiFPConfigs.defaultFPConfig

  /*
  // Create your own configs here
  val defaultSparseConfig = GemminiArrayConfig[SparseUInt, SparseUInt, SparseUInt](
    opcodes = OpcodeSet.custom3,
    tileRows = 1,
    tileColumns = 1,
    meshRows = 4,
    meshColumns = 4,

    ld_queue_length = 8,
    st_queue_length = 2,
    ex_queue_length = 8,

    reservation_station_entries_ld = 8,
    reservation_station_entries_st = 4,
    reservation_station_entries_ex = 16,

    sp_banks = 4,
    sp_singleported = true,
    acc_banks = 1,
    acc_latency = 2,
    acc_singleported = false,
    acc_sub_banks = -1,
    sp_capacity = CapacityInKilobytes(256),
    shifter_banks = 1, // TODO add separate parameters for left and up shifter banks
    dataflow = Dataflow.BOTH,
    acc_capacity = CapacityInKilobytes(64),
    spad_read_delay = 1,

    dma_maxbytes = 64, // TODO get this from cacheblockbytes
    dma_buswidth = 128, // TODO get this from SystemBusKey
    aligned_to = 1,
    tlb_size = 4,
    use_tlb_register_filter = true,
    max_in_flight_mem_reqs = 16,
    use_dedicated_tl_port = false,
    use_shared_ext_mem = false,
    inputType = SparseUInt(8, 24),
    spatialArrayOutputType = SparseUInt(8, 24),
    accType = SparseUInt(8, 24),

    mvin_scale_args = Some(ScaleArguments((t: SparseUInt, u: SparseUInt) => t * u, 4, SparseUInt(8, 24), -1, identity = "1.0", c_str="((x) * (scale))")),
    mvin_scale_acc_args = Some(ScaleArguments((t: SparseUInt, u: SparseUInt) => t * u, 4, SparseUInt(8, 24), -1, identity = "1.0", c_str="((x) * (scale))")),
    mvin_scale_shared = false,

    acc_scale_args = Some(ScaleArguments((t: SparseUInt, u: SparseUInt) => t * u, 4, SparseUInt(8, 24), -1, identity = "1.0",
      c_str = "((x) * (scale))"
    )),
    acc_read_full_width = true,
    acc_read_small_width = true,

    tile_latency = 1,

    ex_read_from_spad = true,
    ex_read_from_acc = true,
    ex_write_to_spad = true,
    ex_write_to_acc = true,

    hardcode_d_to_garbage_addr = false,

    mesh_output_delay = 0,

    has_training_convs = false,
    has_max_pool = true,
    has_nonlinear_activations = true,

    num_counter = 8,
  )
  */

  /*
  val CPRE581UIntConfig = defaultSparseConfig.copy(
    dataflow = Dataflow.WS,
    inputType = SparseUInt(dataWidth = 8, tagsWidth = 8),
    accType = SparseUInt(dataWidth = 8, tagsWidth = 8),
    spatialArrayOutputType = SparseUInt(dataWidth = 8, tagsWidth = 8),
    tile_latency = 1,
    mvin_scale_args = Some(ScaleArguments((t: SparseUInt, u: SparseUInt) => t * u, 4, SparseUInt(8, 8), -1, identity = "1.0", c_str="((x) * (scale))")),
    mvin_scale_acc_args = Some(ScaleArguments((t: SparseUInt, u: SparseUInt) => t * u, 4, SparseUInt(8, 8), -1, identity = "1.0", c_str="((x) * (scale))")),
    acc_scale_args = Some(ScaleArguments((t: SparseUInt, u: SparseUInt) => t * u, 4, SparseUInt(8, 8), -1, identity = "1.0",
      c_str = "((x) * (scale))"
    )),

    meshRows = 4,
    meshColumns = 4,
    has_training_convs = false,
    has_max_pool =  false,
  )
  
  */

  /* Complex number config for Gemmini tutorial */
  val complexConfig = GemminiArrayConfig[Complex, Float, Float](
    inputType = new Complex(16),
    accType = new Complex(16),

    spatialArrayOutputType = new Complex(16),
  )

    // CPRE 581 Config
  val sparseCPRE581Config = GemminiArrayConfig[SparseInt, Float, Float](
    //dataflow = Dataflow.WS,
    inputType = new SparseInt(16),
    accType = new SparseInt(16),
    spatialArrayOutputType = new SparseInt(16),

    meshRows = 4,
    meshColumns = 4,
    //has_training_convs = false,
    //has_max_pool =  false,
    //has_nonlinear_activations = false
  )

  val testconfig = GemminiArrayConfig[SInt, Float, Float](
    //dataflow = Dataflow.WS,
    inputType = SInt(16.W),
    accType = SInt(16.W),
    spatialArrayOutputType = SInt(16.W),

    meshRows = 4,
    meshColumns = 4,
  )



  val baselineInferenceConfig = defaultConfig.copy(
    has_training_convs = false,
  )

  val highPerfInferenceConfig = defaultConfig.copy(
    meshRows = 32,
    meshColumns = 32,

    has_training_convs = false,

    sp_capacity = CapacityInKilobytes(512),
    acc_capacity = CapacityInKilobytes(128),
  )

  val trainingConfig = defaultFpConfig.copy(
    inputType = Float(expWidth = 8, sigWidth = 24),
    accType = Float(expWidth = 8, sigWidth = 24),

    meshRows = 8,
    meshColumns = 8,

    has_training_convs = true,
    has_max_pool =  false,

    sp_capacity = CapacityInKilobytes(512),
    acc_capacity = CapacityInKilobytes(128),
  )

  val ibertInferenceConfig = defaultConfig.copy(
    has_training_convs = false,
    has_max_pool =  false,
    has_normalizations = true,

    acc_capacity = CapacityInKilobytes(128),
  )

  // Specify which of your custom configs you want to build here
  //val customConfig = baselineInferenceConfig
  val customConfig = sparseCPRE581Config
}


class GemminiCustomConfig[T <: Data : Arithmetic, U <: Data, V <: Data](
  gemminiConfig: GemminiArrayConfig[T,U,V] = GemminiCustomConfigs.customConfig
) extends Config((site, here, up) => {
  case BuildRoCC => up(BuildRoCC) ++ Seq(
    (p: Parameters) => {
      implicit val q = p
      val gemmini = LazyModule(new Gemmini(gemminiConfig))
      gemmini
    }
  )
})

