/* This file contains definitions for modules to convert back and forth between
 * positional binary and gray code; all data is passed in/out as UInts */
package com.boskin.synchronization

import chisel3._

class BinaryToGray(dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val din = Input(UInt(dataWidth.W))
    val dout = Output(UInt(dataWidth.W))
  })

  val grayVec = Wire(Vec(dataWidth, Bool()))
  for (i <- 0 until dataWidth - 1) {
    grayVec(i) := io.din(i) ^ io.din(i + 1)
  }
  grayVec(dataWidth - 1) := io.din(dataWidth - 1)

  io.dout := grayVec.asUInt
}

object BinaryToGray {
  def apply(din: UInt, dataWidth: Int): UInt = {
    val inst = Module(new BinaryToGray(dataWidth))
    inst.io.din := din
    inst.io.dout
  }
}

class GrayToBinary(dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val din = Input(UInt(dataWidth.W))
    val dout = Output(UInt(dataWidth.W))
  })
  
  val binVec = Wire(Vec(dataWidth, Bool()))
  for (i <- 0 until dataWidth - 1) {
    binVec(i) := io.din(i) ^ binVec(i + 1)
  }
  binVec(dataWidth - 1) := io.din(dataWidth - 1)

  io.dout := binVec.asUInt
}

object GrayToBinary {
  def apply(din: UInt, dataWidth: Int): UInt = {
    val inst = Module(new GrayToBinary(dataWidth))
    inst.io.din := din
    inst.io.dout
  }
}
