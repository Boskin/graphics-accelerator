package com.boskin.communications.serdes

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util.log2Ceil

import com.boskin.synchronization.{AsyncFIFO, ShiftRegister}

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

/* Parameter explanation:
 * pktWidth: width of each packet to be serialized
 * fifoDepth: depth of the packet FIFO before serialization
 * continuous: if true, allows the module to continuously transmit bits with no
 * pause between packets
 * defaultVal: if 1 or 0 is given, the serializer will output this value by
 * default if no valid data is being transmitted, otherwise, a validOut signal
 * is added to indicate valid output */
class Serializer(pktWidth: Int, continuous: Boolean,
  defaultVal: Int, fifoDepth: Int) extends Module {

  val counterWidth = log2Ceil(pktWidth)
  val outputValid = (defaultVal != 0) && (defaultVal != 1)
  val defaultValBool = defaultVal != 0

  val io = IO(new SerializerIO(pktWidth, outputValid))

  val fifoInst = Module(new AsyncFIFO(UInt(pktWidth.W), fifoDepth))

  fifoInst.io.rdClk := io.sClk
  fifoInst.io.rdReset := io.sReset

  fifoInst.io.wrClk := clock
  fifoInst.io.wrReset := reset.toBool
  fifoInst.io.wrReq.en := io.validIn
  fifoInst.io.wrReq.data := io.pIn


  withClockAndReset(io.sClk, io.sReset) {
    val shiftRegInst = Module(new ShiftRegister(Bool(), pktWidth, true, true))

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

    shiftRegInst.io.din := false.B
    shiftRegInst.io.en := shiftEn
    shiftRegInst.io.ld := fifoInst.io.rdReq.valid
    shiftRegInst.io.ldVal := fifoInst.io.rdReq.data.toBools

    fifoInst.io.rdReq.en := !fifoInst.io.rdReq.valid && shiftCount <= 1.U

    when (fifoInst.io.rdReq.valid || shiftCount > 0.U) {
      fifoRdEn := false.B
    } .elsewhen (!fifoInst.io.empty) {
      fifoRdEn := true.B
    }

    when (fifoInst.io.rdReq.valid) {
      shiftCount := (pktWidth - 1).U
      fifoRdEn := false.B
      shiftEn := true.B
    } .elsewhen (shiftCount > 0.U) {
      shiftCount := shiftCount - 1.U
      fifoRdEn := false.B
      shiftEn := true.B

      if (outputValid) {
        validOut := true.B
      }
    } .otherwise {
      fifoRdEn := true.B
      shiftEn := false.B

      if (outputValid) {
        validOut := false.B
      }
    }

    // Force a 1-cycle pause if not configured in continuous mode
    /* if (continuous) {
      fifoInst.io.rdReq.en := shiftCount <= 2.U
    } else {
      fifoInst.io.rdReq.en := (shiftCount <= 1.U) && !fifoInst.io.empty
    } */
  }
}


object GenSerializer extends App {
  val pktWidth = 4
  val continuous = true
  val defaultVal = 1

  chisel3.Driver.execute(args, () => new Serializer(pktWidth, continuous,
    defaultVal, 16))
}
