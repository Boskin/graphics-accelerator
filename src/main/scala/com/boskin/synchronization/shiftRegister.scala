/* Generic serial shift register */
package com.boskin.synchronization

import chisel3._

class ShiftRegister(dataWidth: Int, length: Int) extends Module {
  val io = IO(new Bundle {
    val din = Input(UInt(dataWidth.W))
    val en = Input(Bool())
    val dout = Output(UInt(dataWidth.W))
  })

  // Inputs enter at index 0 and travel to higher indexes
  val shiftReg = Reg(Vec(length, UInt(dataWidth.W)))
  when (io.en) {
    shiftReg(0) := io.din
    for (i <- 1 until length) {
      shiftReg(i) := shiftReg(i - 1)
    }
  }
  io.dout := shiftReg(length - 1)
}

// Creates a series of registers to delay signals
object Delay {
  def apply(dataWidth: Int, length: Int, din: UInt): UInt = {
    val inst = Module(new ShiftRegister(dataWidth, length))
    inst.io.din := din
    inst.io.en := true.B
    inst.io.dout
  }
  def apply(length: Int, din: Bool): Bool = {
    val inst = Module(new ShiftRegister(1, length))
    inst.io.din := din.asUInt
    inst.io.en := true.B
    inst.io.dout === 1.U
  }
}
