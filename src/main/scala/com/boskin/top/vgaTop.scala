package com.boskin.top

import chisel3._
import chisel3.core.{withClock, withClockAndReset}

import com.boskin.graphicsDriver._
import com.boskin.synchronization.{CDC, ClkGen}

class VGAIO(val colorWidth: Int) extends Bundle {
  val r = Output(UInt(colorWidth.W))
  val g = Output(UInt(colorWidth.W))
  val b = Output(UInt(colorWidth.W))
  val hsync = Output(Bool())
  val vsync = Output(Bool())
}

case class ClockInfo(val sysClk: Double, val gClk: Double)

class VGATop(clkInfo: ClockInfo, timeSpec: VGATiming, colorWidth: Int,
  memRdLatency: Int) extends Module {

  val pixelWidth: Int = colorWidth * 3

  val io = IO(new VGAIO(colorWidth))

  val gClkGenInst = Module(new ClkGen(clkInfo.sysClk, clkInfo.gClk))
  val gClk = gClkGenInst.io.clkOut
  val gReset = Wire(Bool())

  withClock(gClk) {
    gReset := CDC(reset.toBool)
  }
  
  withClockAndReset(gClk, gReset) {
    val vgaInst = Module(new VGA(timeSpec, pixelWidth, memRdLatency))

    io.r := vgaInst.io.pixelOut.r
    io.g := vgaInst.io.pixelOut.g
    io.b := vgaInst.io.pixelOut.b

    io.hsync := vgaInst.io.hsync
    io.vsync := vgaInst.io.vsync

    vgaInst.io.pixelIn.r := 15.U
    vgaInst.io.pixelIn.g := 5.U
    vgaInst.io.pixelIn.b := 15.U

    vgaInst.io.en := true.B
  }
}

object GenVGATop extends App {
  val clkInfo: ClockInfo = ClockInfo(50.0e6, 25.0e6)
  val timeSpec640x480: VGATiming = VGATiming(
    Timing(640, 16, 96, 48),
    Timing(480, 10, 2, 33)
  )
  val memRdLatency: Int = 2
  val colorWidth: Int = 4

  chisel3.Driver.execute(args, () => new VGATop(clkInfo, timeSpec640x480,
    colorWidth, memRdLatency))
}
