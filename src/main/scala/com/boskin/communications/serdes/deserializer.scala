package com.boskin.communications.serdes

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util._

import com.boskin.synchronization.{AsyncFIFO, ShiftRegister}

class DeserializerIO(val pktWidth: Int) extends Bundle {
  val sClk = Input(Clock())
  val sReset = Input(Bool())

  val sIn = Input(Bool())

  val rdEn = Input(Bool())
  val dataReady = Output(Bool())
  val pOut = Output(UInt(pktWidth.W))
  val validOut = Output(Bool())
}

class Deserializer(pktWidth: Int, startSeq: Seq[Bool], stopSeq: Seq[Bool],
  fifoDepth: Int) extends Module {

  val startSeqWidth = startSeq.length
  val stopSeqWidth = stopSeq.length
  val startSeqCounterWidth = log2Ceil(startSeqWidth)
  val pktCounterWidth = log2Ceil(pktWidth)

  val shiftRegWidth = if (startSeqWidth > pktWidth) {
    startSeqWidth
  } else {
    pktWidth
  }

  val counterWidth = log2Ceil(if (startSeqWidth >= pktWidth &&
    startSeqWidth >= stopSeqWidth) {
      startSeqWidth
    } else if (pktWidth >= startSeqWidth && pktWidth >= stopSeqWidth) {
      pktWidth
    } else {
      stopSeqWidth
    })

  val startSeqUInt = {
    val startSeqVec = VecInit(startSeq.reverse)
    startSeqVec.asUInt
  }

  val io = IO(new DeserializerIO(pktWidth))

  val fifoInst = Module(new AsyncFIFO(UInt(pktWidth.W), fifoDepth))
  val shiftRegInst = withClockAndReset(io.sClk, io.sReset) {
    Module(new ShiftRegister(Bool(), shiftRegWidth, true, true))
  }

  val shiftRegUInt = shiftRegInst.io.shiftReg.asUInt

  // FIFO read port connections
  fifoInst.io.rdClk := clock
  fifoInst.io.rdReset := reset
  fifoInst.io.rdReq.en := io.rdEn
  io.pOut := fifoInst.io.rdReq.data
  io.validOut := fifoInst.io.rdReq.valid
  io.dataReady := ~fifoInst.io.empty

  // (Some) FIFO write port connections
  fifoInst.io.wrClk := io.sClk
  fifoInst.io.wrReset := io.sReset
  fifoInst.io.wrReq.data := shiftRegUInt(pktWidth - 1, 0)


  // sClk domain logic
  withClockAndReset(io.sClk, io.sReset) {

    val idle :: pktShift :: stop :: Nil = Enum(3)
    val state = RegInit(stop)

    val shiftCount = RegInit(0.U(counterWidth.W))

    shiftRegInst.io.din := io.sIn
    shiftRegInst.io.en := true.B

    // Reset logic
    shiftRegInst.io.ld := io.sReset
    shiftRegInst.io.ldVal := VecInit(Seq.fill(shiftRegWidth) {true.B})

    // FIFO write condition
    fifoInst.io.wrReq.en := shiftCount === (pktWidth - 1).U &&
      state === pktShift

    switch (state) {
      is (idle) {
        shiftCount := 0.U
        when (shiftRegUInt(startSeqWidth - 1, 0) === startSeqUInt) {
          state := pktShift
        } .otherwise {
          state := idle
        }
      }
      is (pktShift) {
        when (shiftCount === (pktWidth - 1).U) {
          shiftCount := 0.U
          state := stop
        } .otherwise {
          shiftCount := shiftCount + 1.U
          state := pktShift
        }
      }
      is (stop) {
        when (shiftCount === (stopSeqWidth - 1).U) {
          shiftCount := 0.U
          state := idle
        } .otherwise {
          shiftCount := shiftCount + 1.U
          state := stop
        }
      }
    }
  }
}

object GenDeserializer extends App {
  val startSeq = Seq(false.B)
  val pktWidth = 8
  val stopSeq = Seq(true.B)
  val fifoDepth = 256

  chisel3.Driver.execute(args, () => new Deserializer(pktWidth, startSeq,
    stopSeq, fifoDepth))
}
