package com.boskin.synchronization

import chisel3._

class FrameMemRequest(rowAddrWidth: Int, colAddrWidth: Int) extends Bundle {
  val rowAddr = UInt(rowAddrWidth.W)
  val colAddr = UInt(colAddrWidth.W)
  val en = Bool()
}

