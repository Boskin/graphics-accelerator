package com.boskin.communications.uart

import chisel3._
import chisel3.core.withClock
import chisel3.util._

import com.boskin.communications.{GenericSerial, GenericSerialIO}
import com.boskin.synchronization.CDC

class UARTReq(pktSize: Int, write: Boolean) extends Bundle {
  val pkt = if (write) {
    Input(UInt(pktSize.W))
  } else {
    Output(UInt(pktSize.W))
  }
  /* Input high if a received bit must be deserialized, or if a packet must be
   * sent */
  val req = Input(Bool())
  // High if the transmitter/receiver is ready for a new request
  val ready = Output(Bool())
  // Output high if the last request is processed successfully
  val done = Output(Bool())
}

class UARTIO(pktSize: Int) extends GenericSerialIO {
  val rxReq = new UARTReq(pktSize, false)
  val txReq = new UARTReq(pktSize, true)

  override def cloneType: this.type = {
    new UARTIO(pktSize).asInstanceOf[this.type]
  }
}

class UART(pktSize: Int, rxDepth: Int, fifoDepth: Int)
  extends GenericSerial(new UARTIO(pktSize), pktSize, fifoDepth, fifoDepth) {

  val transInst = Module(new TransmitSubsystem(pktSize))
  // Probably need to expand this
  transInst.io.txReq <> io.txReq
  // Bulk-connect FIFO signals
  transInst.io.fifoRdReq <> txFIFOInst.io.rdReq
  transInst.io.fifoWrReq <> txFIFOInst.io.wrReq
  // More inputs
  transInst.io.fifoFull := txFIFOInst.io.full
  transInst.io.fifoEmpty := txFIFOInst.io.empty

  transInst.io.otherClk := io.otherClk
  transInst.io.otherReset := io.otherReset

  // Raw TX signal
  io.tx := transInst.io.tx

  val recvInst = Module(new ReceiveSubsystem(pktSize, rxDepth)) 

  recvInst.io.rxReq <> io.rxReq
  recvInst.io.fifoRdReq <> rxFIFOInst.io.rdReq
  recvInst.io.fifoWrReq <> rxFIFOInst.io.wrReq
  recvInst.io.fifoEmpty := rxFIFOInst.io.empty

  recvInst.io.otherClk := io.otherClk
  recvInst.io.otherReset := io.otherReset

  recvInst.io.rxSync := rxSync
}

object GenUART extends App {
  val pktSize = 8
  val rxDepth = 32
  val fifoDepth = 32

  chisel3.Driver.execute(args, () => new UART(pktSize, rxDepth, fifoDepth))
}
