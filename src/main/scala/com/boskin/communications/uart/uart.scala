package com.boskin.communications.uart

import chisel3._
import chisel3.core.withClock
import chisel3.util._

import com.boskin.communications.serdes.{Serializer, Deserializer}
import com.boskin.communications.{GenericSerial, GenericSerialIO}
import com.boskin.synchronization.CDC

class UARTReq(pktWidth: Int, write: Boolean) extends Bundle {
  val data = if (write) {
    Input(UInt(pktWidth.W))
  } else {
    Output(UInt(pktWidth.W))
  }

  val en = Input(Bool())
  val valid = if (write) {
    null
  } else {
    Output(Bool())
  }
}

class UARTIO(val pktWidth: Int) extends Bundle {
  val otherClk = Input(Clock())
  val otherReset = Input(Bool())

  val tx = Output(Bool())
  val rx = Input(Bool())

  val rxReq = new UARTReq(pktWidth, false)
  val txReq = new UARTReq(pktWidth, true)

  val dataReady = Output(Bool())
}

class UART(pktWidth: Int, rxDepth: Int, fifoDepth: Int) extends Module {

  val io = IO(new UARTIO(pktWidth))

  val startSeq = Seq(false.B)
  val stopSeq = Seq(true.B)

  val transInst = Module(new Serializer(pktWidth, startSeq, stopSeq, true,
    false, fifoDepth))
  val recvInst = Module(new Deserializer(pktWidth, startSeq, stopSeq, fifoDepth))

  transInst.io.sClk := io.otherClk
  transInst.io.sReset := io.otherReset

  transInst.io.pIn := io.txReq.data
  transInst.io.validIn := io.txReq.en

  io.tx := transInst.io.sOut

  recvInst.io.sClk := io.otherClk
  recvInst.io.sReset := io.otherReset

  // Synchronize RX (possible phase difference)
  recvInst.io.sIn := CDC(io.rx)
  recvInst.io.rdEn := io.rxReq.en

  io.rxReq.data := recvInst.io.pOut
  io.rxReq.valid := recvInst.io.validOut

  io.dataReady := recvInst.io.dataReady
}

object GenUART extends App {
  val pktWidth = 8
  val rxDepth = 32
  val fifoDepth = 32

  chisel3.Driver.execute(args, () => new UART(pktWidth, rxDepth, fifoDepth))
}
