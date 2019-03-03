package com.boskin.communications.uart

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util._

import com.boskin.synchronization.{AsyncFIFOReq, ShiftRegister}
import com.boskin.memory.FIFO

class TransmitSubsystemIO(val pktSize: Int) extends Bundle {
  val txReq = new UARTReq(pktSize, true)
  val fifoRdReq = Flipped(new AsyncFIFOReq(UInt(pktSize.W), false))
  val fifoEmpty = Input(Bool())
  val fifoWrReq = Flipped(new AsyncFIFOReq(UInt(pktSize.W), true))
  val fifoFull = Input(Bool())
  val tx = Output(Bool())
  val otherClk = Input(Clock())
  val otherReset = Input(Bool())
}

class TransmitSubsystem(pktSize: Int) extends Module {
  val bitCountWidth = log2Ceil(pktSize + 1)
  val io = IO(new TransmitSubsystemIO(pktSize))


  /************/
  /* TX logic */
  /************/
  withClockAndReset (io.otherClk, io.otherReset) {
    /* States for transmit FSM:
     * idle: waiting for transmit request
     * transData: shifting data into transmit FIFO */
    val idle :: startBit :: trans :: Nil = Enum(3)
    val state = RegInit(idle)

    // FSM outputs
    val shiftRegEn = RegInit(false.B)
    val fifoRdEn = RegInit(false.B)
    val txReg = RegInit(true.B);
    val transBitCount = Reg(UInt(bitCountWidth.W))

    val txBitShiftRegInst = Module(new ShiftRegister(Bool(), pktSize, true))
    txBitShiftRegInst.io.din := false.B
    txBitShiftRegInst.io.ldVal := VecInit(io.fifoRdReq.data.toBools)
    txBitShiftRegInst.io.ld := io.fifoRdReq.valid

    // IO assignments
    txBitShiftRegInst.io.en := shiftRegEn
    io.fifoRdReq.en := fifoRdEn
    io.tx := txReg

    switch (state) {
      is (idle) {
        when (!io.fifoEmpty) {
          fifoRdEn := true.B
        }

        when (fifoRdEn) {
          fifoRdEn := false.B
          state := startBit
        }

      }

      is (startBit) {
        txReg := false.B
        transBitCount := 0.U
        shiftRegEn := true.B
        state := trans
      }

      is (trans) {
        when (transBitCount === pktSize.U) {
          shiftRegEn := false.B
          txReg := true.B
          state := idle
        } .otherwise {
          txReg := txBitShiftRegInst.io.dout
          transBitCount := transBitCount + 1.U
        }
      }
    }
  }


  /**************************/
  /* User-side packet logic */
  /**************************/
  io.fifoWrReq.en := io.txReq.req
  io.fifoWrReq.data := io.txReq.pkt

  io.txReq.ready := ~io.fifoFull
  io.txReq.done := io.fifoWrReq.valid
}
