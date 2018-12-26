/* Simple bundle to hold color signals */
package com.boskin.graphicsDriver

import chisel3._
import chisel3.util.Cat

class Color(rWidth: Int, gWidth: Int, bWidth: Int, alphaWidth: Int)
  extends Bundle {

  val r = UInt(rWidth.W)
  val g = UInt(gWidth.W)
  val b = UInt(bWidth.W)
  val a = if (alphaWidth > 0) {
    UInt(alphaWidth.W)
  } else {
    null
  }

  // Concatenate the colors into a single UInt signal
  def toBGR: UInt = {
    val gr = Cat(g, r)
    val bgr = Cat(b, gr)
    val bgra = if (alphaWidth > 0) {
      Cat(a, bgr)
    } else {
      bgr
    }
    bgra
  }
}

object Color {
  // Create a color with uniform channel widths
  def apply(colorWidth: Int) = {
    val ret = new Color(colorWidth, colorWidth, colorWidth, 0)
    ret
  }
}
