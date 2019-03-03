package com.boskin.communications

import chisel3._
import chisel3.core.{withClock, withClockAndReset}

import com.boskin.synchronization.{AsyncFIFO, CDC}

class GenericSerialIO extends Bundle {
  val otherClk = Input(Clock())
  val otherReset = Input(Bool())

  val rx = Input(Bool())
  val tx = Output(Bool())
}

class GenericSerial[T <: GenericSerialIO](gen: T, pktSize: Int,
  txFIFODepth: Int, rxFIFODepth: Int) extends Module {

  val io = IO(
    gen
  )

  val rxFIFOInst = Module(new AsyncFIFO(Bool(), rxFIFODepth))
  rxFIFOInst.io.rdClk := clock
  rxFIFOInst.io.rdReset := reset

  rxFIFOInst.io.wrClk := io.otherClk
  rxFIFOInst.io.wrReset := io.otherReset

  val rxSync = CDC(io.rx)
  rxFIFOInst.io.wrReq.data := rxSync
  
  // TX FIFO 
  val txFIFOInst = Module(new AsyncFIFO(UInt(pktSize.W), txFIFODepth))
  txFIFOInst.io.rdClk := io.otherClk
  txFIFOInst.io.rdReset := io.otherReset

  txFIFOInst.io.wrClk := clock
  txFIFOInst.io.wrReset := reset
  // Implement transmitter control logic in subclass
}
