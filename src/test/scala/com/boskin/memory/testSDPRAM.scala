package com.boskin.memory

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.PeekPokeTester

import scala.util.Random

class TestSDPRAM(dut: SDPRAM[UInt], dataWidth: Int, size: Int)
  extends PeekPokeTester(dut) {

  val randGen: Random = new Random
  val dataLimit: Int = 1 << dataWidth

  for (i <- 0 until 10) {
    var randData = randGen.nextInt(dataLimit)
    var randAddr = randGen.nextInt(size)

    // Write random data
    poke(dut.io.wrReq.en, 1)
    poke(dut.io.rdReq.en, 0)
    poke(dut.io.wrReq.addr, randAddr)
    poke(dut.io.wrReq.data, randData)
    
    step(1)

    expect(dut.io.wrReq.valid, 1)

    // Read from that address
    poke(dut.io.wrReq.en, 0)
    poke(dut.io.rdReq.en, 1)
    poke(dut.io.rdReq.addr, randAddr)

    step(1)

    // Expect random data
    expect(dut.io.rdReq.data, randData)
    expect(dut.io.rdReq.valid, 1)
  }
}

object TestSDPRAMMain extends App {
  val dataWidth: Int = 8
  val size: Int = 256
  val gen: UInt = UInt(dataWidth.W)

  chisel3.iotesters.Driver.execute(args, () => new SDPRAM(gen, size,
    false ,false)) {
    dut => new TestSDPRAM(dut, dataWidth, size)
  }
}
