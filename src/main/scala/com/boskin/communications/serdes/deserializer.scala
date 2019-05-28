package com.boskin.communications.serdes

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util.log2Ceil

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

  val startSeqWidth = startSeq.width
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

  val io = IO(new Deserializer(pktWidth))

  val fifoInst = Module(new AsyncFIFO(UInt(pktWidth.W), fifoDepth))
  val shiftRegInst = withClockAndReset(io.sClk, io.sReset) {
    Module(new ShiftRegister(Bool(), shiftRegWidth, true, true))
  }

  val shiftRegUInt = shiftRegInst.io.shiftReg.asUInt
  fifoInst.io.wrReq.data := shiftRegUInt(pktWidth - 1, 0)


  withClockAndReset(io.sClk, io.sReset) {
    val idle :: pktShift :: Nil = Enum(2)
    val state = RegInit(idle)
    val nextState = Wire(UInt(2.W))

    shiftRegInst.din := io.sIn
    shiftRegInst.en := true.B
    // Reset
    shiftRegInst.ld := io.sReset
    shiftRegInst.ldVal := VecInit(Seq.fill(shiftRegWidth, false.B))

    switch (state) {
      is (idle) {
        when (shiftRegUInt(startSeqWidth - 1, 0) === startSeqUInt) {
          nextState := pktShift
        } .otherwise {
          nextState := idle
        }
      }
      is (pktShift) {
      }
    }
  }
}
