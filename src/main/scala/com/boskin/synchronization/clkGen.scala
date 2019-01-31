package com.boskin.synchronization

import chisel3._
import chisel3.util.log2Ceil

import scala.math.round

class ClkGenIO extends Bundle {
  val clkOut = Output(Clock())
}

class ClkGen(inFreq: Double, outFreq: Double) extends Module {
  // Number of cycles that needs to be counted before flipping the clock signal
  val countThresh = round(inFreq / (outFreq * 2)).toInt
  val countWidth = log2Ceil(countThresh)

  val io = IO(new ClkGenIO)


  val clkReg = RegInit(false.B)
  io.clkOut := clkReg.asClock

  // If the width is 0, then divide the clock by 2
  if (countWidth > 0) {
    val counter = RegInit(0.U(countWidth.W))
    // Flip the bit everytime the threshold is about to be reached
    when (counter === (countThresh - 1).U) {
      counter := 0.U
      clkReg := ~clkReg
    } .otherwise {
      counter := counter + 1.U
    }
  } else {
    clkReg := ~clkReg
  }
}

object GenClkGen extends App {
  val inFreq: Double = 10.0
  val outFreq: Double = 1.0

  chisel3.Driver.execute(args, () => new ClkGen(inFreq, outFreq))
}
