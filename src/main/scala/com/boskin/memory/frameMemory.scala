package com.boskin.memory

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util.{Cat, log2Ceil}

// Memory request bundle
class FrameMemReq[T <: Data](gen: T, rowWidth: Int, colWidth: Int,
  write: Boolean) extends Bundle {

  // Valid input request
  val en = Input(Bool())
  val row = Input(UInt(rowWidth.W))
  val col = Input(UInt(colWidth.W))
  // Success flag after request
  val valid = Output(Bool())
  val data = if (write) {
    Input(gen)
  } else {
    Output(gen)
  }
}

class FrameMemory[T <: Data](gen: T, rows: Int, cols: Int, rdAsync: Boolean)
  extends Module {

  val size: Int = rows * cols

  val rowAddrWidth: Int = log2Ceil(rows)
  val colAddrWidth: Int = log2Ceil(cols)

  val io = IO(new Bundle {
    // Asynchronous signals
    val rdClk = if (rdAsync) {
      Input(Clock())
    } else {
      null
    }
    val rdReset = if (rdAsync) {
      Input(Bool())
    } else {
      null
    }

    // Requests
    val rdReq = new FrameMemReq(gen, rowAddrWidth, colAddrWidth, false)
    val wrReq = new FrameMemReq(gen, rowAddrWidth, colAddrWidth, true)
  })

  val sdpramInst = Module(new SDPRAM(gen, size, false, rdAsync))

  sdpramInst.io.rdReq.en := io.rdReq.en
  sdpramInst.io.rdReq.addr := Cat(io.rdReq.row, io.rdReq.col)
  io.rdReq.valid := sdpramInst.io.rdReq.valid
  io.rdReq.data := sdpramInst.io.rdReq.data

  if (rdAsync) {
    sdpramInst.io.rdClk := io.rdClk
    sdpramInst.io.rdReset := io.rdReset
  }

  sdpramInst.io.wrReq.en := io.wrReq.en
  sdpramInst.io.wrReq.addr := Cat(io.wrReq.row, io.wrReq.col)
  io.wrReq.valid := sdpramInst.io.wrReq.valid
  sdpramInst.io.wrReq.data := io.wrReq.data
}

object FrameMemoryGen extends App {
  val gen: UInt = UInt(8.W)
  val rows: Int = 16
  val cols: Int = 16
  val rdAsync: Boolean = false
  chisel3.Driver.execute(args, () => new FrameMemory(gen, rows, cols, rdAsync))
}
