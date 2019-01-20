package com.boskin.synchronization

import chisel3._
import chisel3.core.{withClock, withClockAndReset}
import chisel3.util.log2Ceil

class AsyncFIFOReq[T <: Data](gen: T, write: Boolean) extends Bundle {
  val en = Input(Bool())
  val data = if (write) {
    Input(gen)
  } else {
    Output(gen)
  }
  val valid = Output(Bool())
}

class AsyncFIFO[T <: Data](gen: T, depth: Int) extends Module {
  // Local parameters
  // Add an extra bit to the pointers for full/empty logic
  val ptrWidth = log2Ceil(depth) + 1
  // Depth must be a power of 2
  val trueDepth = 1 << (ptrWidth - 1)
  
  // IO
  val io = IO(new Bundle {
    val rdClk = Input(Clock())
    val rdReset = Input(Bool())
    val rdReq = new AsyncFIFOReq(gen, false)
    val empty = Output(Bool())

    val wrClk = Input(Clock())
    val wrReset = Input(Bool())
    val wrReq = new AsyncFIFOReq(gen, true)
    val full = Output(Bool())
  })

  val fifoMem = withClock(io.wrClk) {
    Mem(trueDepth, gen)
  }

  val rdPtrGray = withClockAndReset(io.rdClk, io.rdReset) {
    RegInit(0.U(ptrWidth.W))
  }

  val wrPtrGray = withClockAndReset(io.wrClk, io.wrReset) {
    RegInit(0.U(ptrWidth.W))
  }

  /* Function that synchronizes a pointer to another clock domain and convers it
   * to binary */
  val ptrSyncConv = (ptr: UInt) => {
    GrayToBinary(CDC(ptr, UInt(ptrWidth.W)), ptrWidth)
  }

  // Read logic
  withClockAndReset(io.rdClk, io.rdReset) {
    val wrPtrSyncBin = ptrSyncConv(wrPtrGray)
    val rdPtrBin = GrayToBinary(rdPtrGray, ptrWidth)
    val empty = wrPtrSyncBin === rdPtrBin
    io.empty := empty

    val rdData = Reg(gen)
    io.rdReq.data := rdData

    when (io.rdReq.en && !empty) {
      rdData := fifoMem(rdPtrGray)
      rdPtrGray := BinaryToGray(rdPtrBin + 1.U, ptrWidth)
      io.rdReq.valid := RegNext(true.B)
    } .otherwise {
      io.rdReq.valid := RegNext(false.B)
    }
  }

  // Write logic
  withClockAndReset(io.wrClk, io.wrReset) {
    val rdPtrSyncBin = ptrSyncConv(rdPtrGray)
    val wrPtrBin = GrayToBinary(wrPtrGray, ptrWidth)
    val full = rdPtrSyncBin(ptrWidth - 2, 0) === wrPtrBin(ptrWidth - 2, 0) &&
      rdPtrSyncBin(ptrWidth - 1) =/= wrPtrBin(ptrWidth - 1)
    io.full := full

    when (io.wrReq.en && !full) {
      fifoMem(wrPtrGray) := io.wrReq.data
      wrPtrGray := BinaryToGray(wrPtrBin + 1.U, ptrWidth)
      io.wrReq.valid := RegNext(true.B)
    } .otherwise {
      io.wrReq.valid := RegNext(false.B)
    }
  }
}

object GenAsyncFIFO extends App {
  val gen = UInt(4.W)
  val depth = 16

  chisel3.Driver.execute(args, () => new AsyncFIFO(gen, depth))
}
