/* Black box for an initializable memory with 1 read port and 1 write port,
 * for FPGAs */
package com.boskin.memory

import chisel3._
import chisel3.util.log2Ceil
import chisel3.core.BlackBox
import chisel3.experimental._

class SDPRAMBlackBox(dataWidth: Int, depth: Int, initFile: String)
  extends BlackBox(Map(
    "DATA_W" -> dataWidth,
    "DEPTH" -> depth,
    "INIT_FILE" -> initFile
  )) {

  val addrWidth = log2Ceil(depth)

  val io = IO(new Bundle {
    val rdClk = Input(Clock())
    val rdEn = Input(Bool())
    val rdAddr = Input(UInt(addrWidth.W))
    val rdData = Output(UInt(dataWidth.W))

    val wrClk = Input(Clock())
    val wrEn = Input(Bool())
    val wrAddr = Input(UInt(addrWidth.W))
    val wrData = Input(UInt(dataWidth.W))
  })
}

