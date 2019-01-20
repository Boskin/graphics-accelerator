package com.boskin.communications.uart

import chisel3._
import chisel3.core.withClock
import chisel3.util._

import com.boskin.synchronization.AsyncFIFOReq

class ReceiveSubsystemIO(pktSize: Int) extends Bundle {
  val rxReq = new UARTReq(pktSize, false)
  val fifoRdReq = Flipped(new AsyncFIFOReq(Bool(), false))
  val fifoWrReq = Flipped(new AsyncFIFOReq(Bool(), true))
  val rxSync = Input(Bool())
  val otherClk = Input(Clock())
  override def cloneType: this.type = {
    new ReceiveSubsystemIO(pktSize).asInstanceOf[this.type]
  }
}

class ReceiveSubsystem(pktSize: Int) extends Module {
  val bitCountWidth = log2Ceil(pktSize)

  val io = IO(new ReceiveSubsystemIO(pktSize))


  withClock (io.otherClk) {
    val idle :: recvData :: Nil = Enum(2)
    val state = RegInit(idle)
    val bitCount = RegInit(0.U(bitCountWidth.W))

    val wrEn = RegInit(false.B)
    io.fifoWrReq.en := wrEn

    switch (state) {
      is (idle) {
        when (!io.rxSync) {
          state := recvData
          bitCount := 0.U
          wrEn := true.B
        }
      }
      is (recvData) {
        when (bitCount === (pktSize - 1).U) {
          state := idle
          wrEn := false.B
        } .otherwise {
          bitCount := bitCount + 1.U
        }
      }
    }
  }

  // Master read
  val idle :: deserialize :: Nil = Enum(2)
  
  val state = RegInit(idle)
  val bitCount = RegInit(0.U(bitCountWidth.W))

  val rdEn = RegInit(false.B)

  switch (state) {
    is (idle) {
    }

    is (deserialize) {
    }
  }
  // Simply hook up the RX FIFO's read port to the 
  io.rxReq.pkt := io.fifoRdReq.data
  // Enable the read, if requested
  io.fifoRdReq.en := io.rxReq.req
  // Indicate to the master that the peripheral is ready if there is data 
  io.rxReq.done := io.fifoRdReq.valid
}
