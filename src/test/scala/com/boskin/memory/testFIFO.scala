package com.boskin.memory

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.PeekPokeTester

import scala.collection.mutable.Queue
import scala.util.Random

class TestFIFO(dut: FIFO[UInt], dataWidth: Int, depth: Int)
  extends PeekPokeTester(dut) {

  val randGen = new Random
  val q = new Queue[Int]
  val dataLim = 1 << dataWidth

  var randData = randGen.nextInt(dataLim)
  
  poke(dut.io.wrReq.en, 1)
  poke(dut.io.wrReq.data, randData)

  step(1)

  expect(dut.io.wrReq.valid, 1)
  expect(dut.io.count, 1)

  poke(dut.io.wrReq.en, 0)
  poke(dut.io.rdReq.en, 1)
  
  step(1)

  expect(dut.io.rdReq.valid, 1)
  expect(dut.io.rdReq.data, randData)
  expect(dut.io.count, 0)
  expect(dut.io.empty, 1)
  expect(dut.io.full, 0)

  poke(dut.io.rdReq.en, 0)
  poke(dut.io.wrReq.en, 1)

  // Fill up the software queue
  for (i <- 0 until depth) {
    randData = randGen.nextInt(dataLim)

    q.enqueue(randData)
    poke(dut.io.wrReq.data, randData)

    step(1)

    expect(dut.io.count, i + 1)
    expect(dut.io.wrReq.valid, 1)
  }
  expect(dut.io.full, 1)

  poke(dut.io.rdReq.en, 1)

  for (i <- 0 until 256) {
    randData = randGen.nextInt(dataLim)

    q.enqueue(randData)
    poke(dut.io.wrReq.data, randData)

    step(1)

    expect(dut.io.count, depth)
    expect(dut.io.wrReq.valid, 1)
    expect(dut.io.rdReq.valid, 1)
    expect(dut.io.rdReq.data, q.dequeue())
    expect(dut.io.full, 1)
  }

  poke(dut.io.wrReq.en, 0)

  for (i <- 0 until depth) {
    step(1)

    expect(dut.io.count, depth - i - 1)
    expect(dut.io.rdReq.valid, 1)
    expect(dut.io.rdReq.data, q.dequeue())
  }
  expect(dut.io.empty, 1)
}

object TestFIFOMain extends App {
  val dataWidth = 4
  val gen = UInt(dataWidth.W)
  val depth = 8

  chisel3.iotesters.Driver.execute(args, () => new FIFO(gen, depth)) {
    dut => new TestFIFO(dut, dataWidth, depth)
  }
}
