/* Generic serial shift register */
package com.boskin.synchronization

import chisel3._

class ShiftRegisterIO[T <: Data](private val gen: T, val length: Int,
  val load: Boolean, val exposeReg: Boolean) extends Bundle {

  val din = Input(gen)
  val en = Input(Bool())
  val ld = if (load) {
    Input(Bool())
  } else {
    null
  }

  val ldVal = if (load) {
    Input(Vec(length, gen))
  } else {
    null
  }

  val shiftReg = if (exposeReg) {
    Output(Vec(length, gen))
  } else {
    null
  }

  val dout = Output(gen)
}

class ShiftRegister[T <: Data](gen: T, length: Int, load: Boolean = false,
  exposeReg: Boolean = false) extends Module {

  val io = IO(new ShiftRegisterIO(gen, length, load, exposeReg))

  // Inputs enter at index 0 and travel to higher indexes
  val shiftReg = Reg(Vec(length, gen))

  def shiftLogic(): Unit = {
    shiftReg(0) := io.din
    for (i <- 1 until length) {
      shiftReg(i) := shiftReg(i - 1)
    }
  }

  def loadLogic(): Unit = {
    shiftReg := io.ldVal
  }

  if (load) {
    when (io.ld) {
      loadLogic()
    } .elsewhen (io.en) {
      shiftLogic()
    }
  } else {
    when (io.en) {
      shiftLogic()
    }
  }

  if (exposeReg) {
    io.shiftReg := shiftReg
  }

  io.dout := shiftReg(length - 1)
}

// Creates a series of registers to delay signals that are always enabled
object Delay {
  def apply[T <: Data](gen: T, length: Int, din: T): T = {
    val inst = Module(new ShiftRegister(gen, length))
    inst.io.din := din
    inst.io.en := true.B
    inst.io.dout
  }
}

// Simple CDC module, instantiate this in the destination clock domain
object CDC {
  def apply[T <: Data](din: T, gen: T, length: Int): T = {
    val inst = Module(new ShiftRegister(gen, length))
    inst.io.din := din
    inst.io.en := true.B
    inst.io.dout
  }
  def apply[T <: Data](din: T, gen: T): T = {
    CDC(din, gen, 2)
  }
  def apply(din: Bool): Bool = {
    CDC(din, Bool(), 2)
  }
}
