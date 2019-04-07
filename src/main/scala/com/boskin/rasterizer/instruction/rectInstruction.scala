package com.boskin.rasterizer.instruction

import chisel3._
import chisel3.util.log2Ceil

class RectInstruction(coordWidth: Int) extends Instruction(coordWidth) {
  val x0 = UInt(coordWidth.W)
  val y0 = UInt(coordWidth.W)
  val x1 = UInt(coordWidth.W)
  val y1 = UInt(coordWidth.W)
}
