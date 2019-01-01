package com.boskin.memory

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util.log2Ceil

// Bundle of request signals
class SDPRAMReq[T <: Data](gen: T, addrWidth: Int, write: Boolean)
  extends Bundle {

  val en = Input(Bool())
  val addr = Input(UInt(addrWidth.W))
  val valid = Output(Bool())
  val data = if (write) {
    Input(gen)
  } else {
    Output(gen)
  }
}

class SDPRAM[T <: Data](gen: T, size: Int, wrAsync: Boolean, rdAsync: Boolean)
  extends Module {
  val addrWidth: Int = log2Ceil(size)
  val io = IO(new Bundle {
    val rdReq = new SDPRAMReq(gen, addrWidth, false)
    val wrReq = new SDPRAMReq(gen, addrWidth, true)
    
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

    val wrClk = if (wrAsync) {
      Input(Clock())
    } else {
      null
    }
    val wrReset = if (wrAsync) {
      Input(Bool())
    } else {
      null
    }
  })

  val mem = Mem(size, gen)

  def wrLogic(): Unit = {
    when (io.wrReq.en) {
      mem.write(io.wrReq.addr, io.wrReq.data)
    }
    io.wrReq.valid := RegNext(io.wrReq.en) 
  }

  def rdLogic(): Unit = {
    val rdMem = mem.read(io.rdReq.addr)
    io.rdReq.data := RegNext(rdMem)
    io.rdReq.valid := RegNext(io.rdReq.en)
  }

  if (wrAsync) {
    withClockAndReset(io.wrClk, io.wrReset) {
      wrLogic()
    }
  } else {
    wrLogic()
  }

  if (rdAsync) {
    withClockAndReset(io.rdClk, io.rdReset) {
      rdLogic()
    }
  } else {
    rdLogic()
  }
}

object SDPRAMGen extends App {
  val gen: UInt = UInt(8.W)
  val size: Int = 256
  chisel3.Driver.execute(args, () => new SDPRAM(gen, size, true, true))
}
