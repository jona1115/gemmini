// See README.md for license details.
//import sbt.project

//addSbtPlugin("edu.berkeley.cs" % "chisel3-sbt-plugin" % "3.5.0")

name := "gemmini"

version := "3.1.0"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chisel3" % "3.5.6",
  //"edu.berkeley.cs" %% "rocketchip" % "1.2.+",
  "edu.berkeley.cs" %% "chisel-iotesters" % "2.5.6",
  "org.scalanlp" %% "breeze" % "1.1", 
  //_
  "edu.berkeley.cs" % "rocketchip_2.13" % "1.2.0-SNAPSHOT",


  // "org.specs2" %% "specs2-core" % "4.10.6" % Test,
  // "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
  // "junit" % "junit" % "4.13.2" % Test,
  // "org.scalameta" %% "munit" % "0.7.29" % Test,
  // "dev.zio" %% "zio-test" % "2.0.0" % Test,
  // "dev.zio" %% "zio-test-sbt" % "2.0.0" % Test
    
  )

  // testFrameworks in Test ++= Seq(
  //   new TestFramework("org.specs2.runner.SpecsFramework"),  // For Specs2
  //   new TestFramework("com.novocode.junit.JUnitFramework"), // For JUnit
  //   new TestFramework("munit.Framework"),                   // For MUnit
  //   new TestFramework("zio.test.sbt.ZTestFramework")        // For ZIO Test
  // )

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  Resolver.mavenLocal
)
  

//lazy val gemmini = (project in file("generators/gemmini"))

//   .settings(

//   name := "gemmini",

//   version := "3.1.0",

//   scalaVersion := "2.13.10",

//   libraryDependencies ++= Seq(
//   //"edu.berkeley.cs" %% "chisel3" % "3.5.6",
//   "edu.berkeley.cs" %% "chisel3" % "3.5.6",
//   "edu.berkeley.cs" %% "rocketchip" % "1.6.+",
//   //"edu.berkeley.cs" % "rocketchip_2.13" % "1.2.0-SNAPSHOT", //%% "rocketchip" % "1.2.+
//   //"edu.berkeley.cs" %% "chisel-iotesters" % "2.6.+",
//   "org.scalatest" %% "scalatest" % "3.1.+" % Test,
//   "org.scalanlp" %% "breeze" % "1.1"),
  
//   //enablePlugins(ChiselPlugin),
// //addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full)
// // val chiselVersion = "6.5.0"

// resolvers ++= Seq(
//   Resolver.sonatypeRepo("snapshots"),
//   Resolver.sonatypeRepo("releases"),
//   Resolver.mavenLocal
//   )
//   )
// specified commit BEFORE scala bump to 2.13 for compatibility
// need this version for MulRecFN and fast divider
//lazy val newHardfloat = RootProject(uri("https://github.com/ucb-bar/berkeley-hardfloat.git#74cc28"))
