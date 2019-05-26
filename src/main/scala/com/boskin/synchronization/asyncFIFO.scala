package com.boskin.synchronization

import chisel3._
import chisel3.core.{withClock, withClockAndReset}
import chisel3.util.log2Ceil

class AsyncFIFOReq[T <: Data](private val gen: T, val write: Boolean)
  extends Bundle {

  val en = Input(Bool())
  val data = if (write) {
    Input(gen)
  } else {
    Output(gen)
  }
  val valid = Output(Bool())
}

class AsyncFIFOIO[T <: Data](private val gen: T, val countWidth: Int)
  extends Bundle {

  val rdClk = Input(Clock())
  val rdReset = Input(Bool())
  val rdReq = new AsyncFIFOReq(gen, false)
  val empty = Output(Bool())
  val rdCount = Output(UInt(countWidth.W))

  val wrClk = Input(Clock())
  val wrReset = Input(Bool())
  val wrReq = new AsyncFIFOReq(gen, true)
  val full = Output(Bool())
  val wrCount = Output(UInt(countWidth.W))
}

class AsyncFIFO[T <: Data](gen: T, depth: Int) extends Module {
  // Local parameters
  val countWidth = log2Ceil(depth)
  // Add an extra bit to the pointers for full/empty logic
  val ptrWidth = countWidth +  1
  // Depth must be a power of 2
  val trueDepth = 1 << countWidth
  
  // IO
  val io = IO(new AsyncFIFOIO(gen, countWidth))

  // Helper function for advancing a pointer
  def ptrDiff(ptr1: UInt, ptr2: UInt, pWidth: Int): UInt = {
    val res = UInt(pWidth.W)
    when (ptr1 > ptr2) {
      res := ptr1 - ptr2
    } .otherwise {
      res := ptr2 - ptr1
    }
    res
  }

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

    io.empty := wrPtrSyncBin === rdPtrBin

    val rdCount = RegInit(0.U(countWidth.W))
    io.rdCount := rdCount

    val rdData = Reg(gen)
    io.rdReq.data := rdData

    val valid = RegInit(false.B)
    valid := io.rdReq.en && !io.empty
    io.rdReq.valid := valid

    when (io.rdReq.en && !io.empty) {
      rdData := fifoMem(rdPtrGray)
      rdPtrGray := BinaryToGray(rdPtrBin + 1.U, ptrWidth)
      rdCount := wrPtrSyncBin - (rdPtrBin + 1.U)
    } .otherwise {
      rdCount := wrPtrSyncBin - rdPtrBin
    }
  }

  // Write logic
  withClockAndReset(io.wrClk, io.wrReset) {
    val rdPtrSyncBin = ptrSyncConv(rdPtrGray)
    val wrPtrBin = GrayToBinary(wrPtrGray, ptrWidth)
    io.full := rdPtrSyncBin(ptrWidth - 2, 0) ===
      wrPtrBin(ptrWidth - 2, 0) &&
      rdPtrSyncBin(ptrWidth - 1) =/= wrPtrBin(ptrWidth - 1)

    val valid = RegInit(false.B)
    valid := io.wrReq.en && !io.full
    io.wrReq.valid := valid

    val wrCount = RegInit(0.U(countWidth.W))
    io.wrCount := wrCount

    when (io.wrReq.en && !io.full) {
      fifoMem(wrPtrGray) := io.wrReq.data
      wrPtrGray := BinaryToGray(wrPtrBin + 1.U, ptrWidth)
      wrCount := wrPtrBin - rdPtrSyncBin + 1.U
    } .otherwise {
      wrCount := wrPtrBin - rdPtrSyncBin
    }
  }
}

object GenAsyncFIFO extends App {
  val gen = UInt(4.W)
  val depth = 16

  chisel3.Driver.execute(args, () => new AsyncFIFO(gen, depth))
}
