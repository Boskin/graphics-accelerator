/* Simple module that facilitates 1 bit CDC, for multi-bit CDC, it is better to
 * use an asynchronous FIFO */
package com.boskin.synchronization

import chisel3._
import chisel3.core.{withClock, withClockAndReset}

class CDC(stages: Int = 2) extends Module {
  val io = IO(new Bundle {
    val din = Input(Bool())
    val destClk = Input(Clock())
    val destReset = Input(Bool())
    val dout = Output(Bool())
  })

  withClockAndReset(io.destClk, io.destReset) {
    val cdc0 = RegNext(io.din)
    val cdc1 = RegNext(cdc0)
    io.dout := cdc1
  }
}

// Creates the default, 2-stage CDC
object CDC {
  def apply(din: Bool, destClk: Clock, destReset: Bool): Bool = {
    val inst = Module(new CDC(2))
    inst.io.din := din
    inst.io.destClk := destClk
    inst.io.destReset := destReset
    inst.io.dout
  }
}
