package com.boskin.communications.serdes

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util.log2Ceil

import com.boskin.synchronization.{AsyncFIFO, ShiftRegister}

// IO class
class SerializerIO(val pktWidth: Int, val outputValid: Boolean) extends Bundle {
  val sClk = Input(Clock())
  val sReset = Input(Bool())

  val pIn = Input(UInt(pktWidth.W))
  val validIn = Input(Bool())

  val sOut = Output(Bool())

  val validOut = if (outputValid) {
    Output(Bool())
  } else {
    null
  }
}

// Class that represents a payload structurally
class Payload(val startSeqWidth: Int, val pktWidth: Int, val stopSeqWidth: Int)
  extends Bundle {

  val startSeq = if (startSeqWidth > 0) {
    Vec(startSeqWidth, Bool())
  } else {
    null
  }

  val pktSeq = if (pktWidth > 0) {
    Vec(pktWidth, Bool())
  } else {
    null
  }

  val stopSeq = if (stopSeqWidth > 0) {
    Vec(stopSeqWidth, Bool())
  } else {
    null
  }
}

/* Parameter explanation:
 * pktWidth: width of each packet to be serialized
 *
 * startSeq: binary sequence to be output before the packet
 *
 * stopSeq: binary sequence to be output after the packet
 *
 * defaultSOut: hold sOut to this value when not transmitting anything
 *
 * outputValid: if true, adds a validOut port, validOut is held high only
 * during the packet transmission (not during startSeq or stopSeq)
 *
 * fifoDepth: depth of the packet FIFO before serialization */
class Serializer(pktWidth: Int, startSeq: Seq[Bool], stopSeq: Seq[Bool],
   defaultSOut: Boolean, outputValid: Boolean, fifoDepth: Int) extends Module {

  val startSeqWidth = startSeq.length
  val stopSeqWidth = stopSeq.length
  val payloadWidth = pktWidth + startSeqWidth + stopSeqWidth
  val counterWidth = log2Ceil(payloadWidth)


  val io = IO(new SerializerIO(pktWidth, outputValid))

  val fifoInst = Module(new AsyncFIFO(UInt(pktWidth.W), fifoDepth))

  fifoInst.io.rdClk := io.sClk
  fifoInst.io.rdReset := io.sReset

  fifoInst.io.wrClk := clock
  fifoInst.io.wrReset := reset.toBool
  fifoInst.io.wrReq.en := io.validIn
  fifoInst.io.wrReq.data := io.pIn


  withClockAndReset(io.sClk, io.sReset) {
    val shiftRegInst = Module(new ShiftRegister(Bool(), payloadWidth, true, true))

    val shiftCount = RegInit(0.U(counterWidth.W))
    val validOut = if (outputValid) {
      RegInit(false.B)
    } else {
      null
    }

    if (outputValid) {
      io.validOut := validOut
    }

    io.sOut := RegNext(shiftRegInst.io.dout)

    val fifoRdEn = RegInit(false.B)
    //fifoInst.io.rdReq.en := fifoRdEn

    val shiftEn = RegInit(false.B)

    // Concatenate the sequences and packet to form the payload
    val payload = Wire(new Payload(startSeqWidth, pktWidth, stopSeqWidth))

    if (startSeqWidth > 0) {
      payload.startSeq := VecInit(startSeq.reverse)
    }

    payload.pktSeq := fifoInst.io.rdReq.data.toBools

    if (stopSeqWidth > 0) {
      payload.stopSeq := VecInit(stopSeq.reverse)
    }

    // Connect shift register IO
    shiftRegInst.io.din := defaultSOut.B
    shiftRegInst.io.en := shiftEn
    shiftRegInst.io.ld := fifoInst.io.rdReq.valid
    shiftRegInst.io.ldVal := VecInit(payload.asUInt.toBools)

    fifoInst.io.rdReq.en := !fifoInst.io.rdReq.valid && shiftCount <= 1.U

    when (fifoInst.io.rdReq.valid) {
      shiftCount := (payloadWidth - 1).U
      shiftEn := true.B
    } .elsewhen (shiftCount > 0.U) {
      shiftCount := shiftCount - 1.U
      shiftEn := true.B
    } .otherwise {
      shiftEn := false.B
    }

    if (outputValid) {
      val shiftCountDelay = RegInit(0.U(counterWidth.W))
      shiftCount := shiftCount
      when (shiftCount < (payloadWidth - 1 - startSeqWidth).U && 
        shiftCountDelay >= stopSeqWidth.U) {

        io.validOut := RegNext(true.B)
      } .otherwise {
        io.validOut := RegNext(false.B)
      }
    }
  }
}


object GenSerializer extends App {
  val pktWidth = 4
  val startSeq = Seq(false.B)
  val stopSeq = Seq(true.B)
  val defaultSOut = true
  val outputValid = false

  chisel3.Driver.execute(args, () => new Serializer(pktWidth, startSeq,
    stopSeq, defaultSOut, outputValid, 16))
}
