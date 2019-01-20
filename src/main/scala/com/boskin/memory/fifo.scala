package com.boskin.memory

import chisel3._
import chisel3.util.log2Ceil

class FIFOReq[T <: Data](gen: T, write: Boolean) extends Bundle {
  val en = Input(Bool())
  val data = if (write) {
    Input(gen)
  } else {
    Output(gen)
  }
  val valid = Output(Bool())
}

class FIFOIO[T <: Data](gen: T, ptrWidth: Int) extends Bundle {
  val rdReq = new FIFOReq(gen, false)
  val empty = Output(Bool())
  val wrReq = new FIFOReq(gen, true)
  val full = Output(Bool())
  val count = Output(UInt((ptrWidth + 1).W))

  override def cloneType: this.type = {
    new FIFOIO(gen, ptrWidth).asInstanceOf[this.type]
  }
}

class FIFO[T <: Data](gen: T, depth: Int) extends Module {
  val ptrWidth = log2Ceil(depth)
  val trueDepth = 1 << ptrWidth
  val io = IO(new FIFOIO(gen, ptrWidth))

  val fifoMem = Mem(trueDepth, gen)

  val rdPtr = RegInit(0.U(ptrWidth.W))
  val wrPtr = RegInit(0.U(ptrWidth.W))
  val count = RegInit(0.U((ptrWidth + 1).W))
  io.count := count

  io.empty := count === 0.U
  io.full := count === trueDepth.U

  val rdData = Reg(gen)
  io.rdReq.data := rdData

  // Procedure to read from the FIFO
  def rdLogic(): Unit = {
    rdData := fifoMem(rdPtr)
    rdValid := true.B
    rdPtr := rdPtr + 1.U
  }

  // Procedure to write to the FIFO
  def wrLogic(): Unit = {
    fifoMem(wrPtr) := io.wrReq.data
    wrValid := true.B
    wrPtr := wrPtr + 1.U
  }

  // Invalidate the requests by default
  val rdValid = RegInit(false.B)
  val wrValid = RegInit(false.B)

  rdValid := false.B
  wrValid := false.B

  io.rdReq.valid := rdValid
  io.wrReq.valid := wrValid

  when (io.rdReq.en && io.wrReq.en && !io.empty) {
    rdLogic()
    wrLogic()
  } .elsewhen (io.rdReq.en && !io.empty) {
    rdLogic()
    count := count - 1.U
  } .elsewhen (io.wrReq.en && !io.full) {
    wrLogic()
    count := count + 1.U
  }
}

object GenFIFO extends App {
  val gen = UInt(4.W)
  val depth = 8
  chisel3.Driver.execute(args, () => new FIFO(gen, depth))
}
