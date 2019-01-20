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

class UART(pktSize: Int, fifoDepth: Int)
  extends GenericSerial(new UARTIO(pktSize), fifoDepth) {

  val transInst = Module(new TransmitSubsystem(pktSize))
  // Probably need to expand this
  transInst.io.txReq <> io.txReq
  // Bulk-connect FIFO signals
  transInst.io.fifoRdReq <> txFIFOInst.io.rdReq
  transInst.io.fifoWrReq <> txFIFOInst.io.wrReq
  // More inputs
  transInst.io.fifoEmpty := txFIFOInst.io.empty
  transInst.io.otherClk := io.otherClk

  // Raw TX signal
  io.tx := transInst.io.tx

}

object GenUART extends App {
  val pktSize = 8
  val fifoDepth = 32

  chisel3.Driver.execute(args, () => new UART(pktSize, fifoDepth))
}
