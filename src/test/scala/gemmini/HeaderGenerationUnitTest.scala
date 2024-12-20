

package gemmini

//import org.scalatest.FlatSpec
import org.scalatest.flatspec.AnyFlatSpec

// class HeaderGenerationUnitTest extends FlatSpec {
class HeaderGenerationUnitTest extends AnyFlatSpec {
  it should "generate a header" in {
    println(GemminiConfigs.defaultConfig.generateHeader())
  }
}
