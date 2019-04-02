package com.boskin

import chisel3._
import chisel3.util._

class SevenSegDecoder(activeLow: Boolean) extends Module {
  val io = IO(new Bundle {
    val din = Input(UInt(4.W))
    val dout = Output(UInt(7.W))
  })

  val decOut = Wire(UInt(7.W))

  if (activeLow) {
    io.dout := ~decOut
  } else {
    io.dout := decOut
  }

  // Default case
  decOut := 0.U
  switch (io.din) {
    is (0.U) {
      decOut := 0.U
    }
    is (1.U) {
      decOut := "h02".U
    }
    is (2.U) {
      decOut := "h5b".U
    }
    is (3.U) {
      decOut := "h4f".U
    }
    is (4.U) {
      decOut := "h66".U
    }
    is (5.U) {
      decOut := "h6d".U
    }
    is (6.U) {
      decOut := "h7d".U
    }
    is (7.U) {
      decOut := "h07".U
    }
    is (8.U) {
      decOut := "h7f".U
    }
    is (9.U) {
      decOut := "h6f".U
    }
    is (10.U) {
      decOut := "h77".U
    }
    is (11.U) {
      decOut := "h7c".U
    }
    is (12.U) {
      decOut := "h5a".U
    }
    is (13.U) {
      decOut := "h5e".U
    }
    is (14.U) {
      decOut := "h79".U
    }
    is (15.U) {
      decOut := "h71".U
    }
  }
}

object GenSevenSegDecoder extends App {
  chisel3.Driver.execute(args, () => new SevenSegDecoder(true))
}
