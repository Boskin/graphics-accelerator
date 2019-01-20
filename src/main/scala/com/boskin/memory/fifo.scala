/* Synchronous, symmetric FIFO. The FIFO depth is required to be a power of 2;
 * if not, the true depth will be rounded up to the next power of 2 */
package com.boskin.memory

import chisel3._
import chisel3.util.log2Ceil

// Read/write request bundle
class FIFOReq[T <: Data](gen: T, write: Boolean) extends Bundle {
  // If high, initiates the request next clock cycle
  val en = Input(Bool())
  // Data associated with the request
  val data = if (write) {
    Input(gen)
  } else {
    Output(gen)
  }
  // Response if the request was fulfilled successfully
  val valid = Output(Bool())
}

// FIFO IO bundle
class FIFOIO[T <: Data](gen: T, ptrWidth: Int) extends Bundle {
  // Read request
  val rdReq = new FIFOReq(gen, false)
  // Empty signal
  val empty = Output(Bool())
  // Write request
  val wrReq = new FIFOReq(gen, true)
  // Full signal
  val full = Output(Bool())
  // Number of data elements in the FIFO
  val count = Output(UInt((ptrWidth + 1).W))

  override def cloneType: this.type = {
    new FIFOIO(gen, ptrWidth).asInstanceOf[this.type]
  }
}

class FIFO[T <: Data](gen: T, depth: Int) extends Module {
  // Width of a pointer
  val ptrWidth = log2Ceil(depth)
  // Depth rounded up to the next power of 2
  val trueDepth = 1 << ptrWidth
  // IO declaration
  val io = IO(new FIFOIO(gen, ptrWidth))

  // FIFO memory bank
  val fifoMem = Mem(trueDepth, gen)

  // Read/write pointers
  val rdPtr = RegInit(0.U(ptrWidth.W))
  val wrPtr = RegInit(0.U(ptrWidth.W))

  // Count register
  val count = RegInit(0.U((ptrWidth + 1).W))
  io.count := count

  // Full and empty logic
  io.empty := count === 0.U
  io.full := count === trueDepth.U

  // Read data register
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

  // Request valid registers
  val rdValid = RegInit(false.B)
  val wrValid = RegInit(false.B)

  // Invalidate the requests by default
  rdValid := false.B
  wrValid := false.B

  io.rdReq.valid := rdValid
  io.wrReq.valid := wrValid

  // Read/write logic
  when (io.rdReq.en && io.wrReq.en && !io.empty) {
    // If both read and write occur, don't update count
    rdLogic()
    wrLogic()
  } .elsewhen (io.rdReq.en && !io.empty) {
    // Only read
    rdLogic()
    count := count - 1.U
  } .elsewhen (io.wrReq.en && !io.full) {
    // Only write
    wrLogic()
    count := count + 1.U
  }
}

object GenFIFO extends App {
  val gen = UInt(4.W)
  val depth = 8
  chisel3.Driver.execute(args, () => new FIFO(gen, depth))
}
