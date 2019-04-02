package com.boskin.top

import chisel3._
import chisel3.core.withClock

import com.boskin.communications.uart.UART
import com.boskin.SevenSegDecoder
import com.boskin.synchronization.{CDC, ClkGen}

class UARTTopIO(val dispWidth: Int) extends Bundle {
  val rx = Input(Bool())
  val tx = Output(Bool())

  val digit0Disp = Output(UInt(dispWidth.W))
  val digit1Disp = Output(UInt(dispWidth.W))
}

class UARTTop(dispActiveLow: Boolean) extends Module {
  val digitWidth = 4
  val dispWidth = 7

  val io = IO(new UARTTopIO(dispWidth))

  val digit0DispInst = Module(new SevenSegDecoder(dispActiveLow))
  val digit1DispInst = Module(new SevenSegDecoder(dispActiveLow))
  
  val clkGenInst = Module(new ClkGen(50.0e6, 115200))

  val digit0Reg = RegInit(0.U(dispWidth.W))
  val digit1Reg = RegInit(0.U(dispWidth.W))

  val uartInst = Module(new UART(2 * digitWidth, 16, 16))

  uartInst.io.otherClk := clkGenInst.io.clkOut
  withClock(clkGenInst.io.clkOut) {
    uartInst.io.otherReset := CDC(reset.toBool)
  }

  uartInst.io.rx := io.rx
  io.tx := uartInst.io.tx

  uartInst.io.rxReq.req := true.B

  uartInst.io.txReq.pkt := 0.U
  uartInst.io.txReq.req := false.B

  when (uartInst.io.rxReq.done) {
    digit0Reg := uartInst.io.rxReq.pkt(digitWidth - 1, 0)
    digit1Reg := uartInst.io.rxReq.pkt(2 * digitWidth - 1, digitWidth)
  }

  digit0DispInst.io.din := digit0Reg
  digit1DispInst.io.din := digit1Reg

  io.digit0Disp := digit0DispInst.io.dout
  io.digit1Disp := digit1DispInst.io.dout
}

object GenUARTTop extends App {
  val dispActiveLow = false

  chisel3.Driver.execute(args, () => new UARTTop(dispActiveLow))
}
