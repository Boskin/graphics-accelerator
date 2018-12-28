package com.boskin.synchronization

import chisel3._
import chisel3.core.{withClockAndReset, withClock}
import chisel3.util.log2Ceil

import com.boskin.graphicsDriver.Color
import com.boskin.graphicsDriver.ColorSpec

class FrameMemory(colorSpec: ColorSpec, rows: Int, cols: Int) extends Module {
  val rowAddrWidth: Int = log2Ceil(rows)
  val colAddrWidth: Int = log2Ceil(cols)

  val io = IO(new Bundle {
    val rdClk = Input(Clock())
    val rdReset = Input(Bool())

    // Requests
    val wrReq = Input(new FrameMemRequest(rowAddrWidth, colAddrWidth))
    val rdReq = Input(new FrameMemRequest(rowAddrWidth, colAddrWidth))

    // If high, prevents reading to avoid synchronization issues
    val wrLock = Input(Bool())

    val wrData = Input(new Color(colorSpec))
    val rdData = Output(new Color(colorSpec))
  })

  val frameMem = Vec(rows, Vec(cols, new Color(colorSpec)))

  // Write logic
  when (io.wrLock && io.wrReq.en) {
    frameMem(io.wrReq.rowAddr)(io.rdReq.colAddr) := io.wrData
  }

  // Read clock domain logic
  withClockAndReset(io.rdClk, io.rdReset) {
    // Synchronize wrLock to the rdClk domain
    val wrLockSync = CDC(io.wrLock)
    val rdDataReg = Reg(new Color(colorSpec))
    io.rdData := rdDataReg

    /* Read logic, make sure reading only happens if nothing is writing to this
     * buffer */
    when (!wrLockSync && io.rdReq.en) {
      rdDataReg := frameMem(io.rdReq.rowAddr)(io.rdReq.colAddr)
    }
  }
}
