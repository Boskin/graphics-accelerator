/* Simple bundle to hold color signals */
package com.boskin.graphicsDriver

import chisel3._
import chisel3.util.Cat

// Structure that contains the widths of the color fields and the total
case class ColorSpec(rWidth: Int, gWidth: Int, bWidth: Int, alphaWidth: Int) {
  val width: Int = rWidth + gWidth + bWidth + alphaWidth
}

class Color(val spec: ColorSpec)
  extends Bundle {

  val r = UInt(spec.rWidth.W)
  val g = UInt(spec.gWidth.W)
  val b = UInt(spec.bWidth.W)
  val a = if (spec.alphaWidth > 0) {
    UInt(spec.alphaWidth.W)
  } else {
    null
  }

  // Concatenate the colors into a single UInt signal
  def toBGR: UInt = {
    val gr = Cat(g, r)
    val bgr = Cat(b, gr)
    val bgra = if (spec.alphaWidth > 0) {
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
    val spec = ColorSpec(colorWidth, colorWidth, colorWidth, 0)
    val ret = new Color(spec)
    ret
  }
}
