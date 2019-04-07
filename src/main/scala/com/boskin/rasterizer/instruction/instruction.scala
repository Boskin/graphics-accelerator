package com.boskin.rasterizer.instruction

import chisel3._
import chisel3.util.{Enum, log2Ceil}

object Instruction {
  val shapeCount = 2
  val rect :: circ :: Nil = Enum(shapeCount)
  val shapeWidth = log2Ceil(shapeCount)
}

class Instruction(val coordWidth: Int) extends Bundle {
  val shape = UInt(Instruction.shapeWidth.W)
}
