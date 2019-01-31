package com.boskin.synchronization

import chisel3.iotesters
import chisel3.iotesters.PeekPokeTester

class TestClkGen(dut: ClkGen, duration: Int) extends PeekPokeTester(dut) {
  step(duration)
}

object TestClkGenMain extends App {
  chisel3.iotesters.Driver.execute(args, () => new ClkGen(30.0, 15.0)) {
    dut => new TestClkGen(dut, 1000)
  }
}
