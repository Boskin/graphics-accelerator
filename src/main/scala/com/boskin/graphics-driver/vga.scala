/* Simple, generic VGA driver */
package com.boskin.graphicsDriver

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util.log2Ceil

import com.boskin.synchronization._

/* Timing info class, the order in which the regions go are as follows:
 * sync => back porch => visible => front porch => sync */
case class Timing(visible: Int, frontPorch: Int, sync: Int, backPorch: Int) {
  // Total length of line
  val total: Int = visible + frontPorch + sync + backPorch

  // Method that returns true if count is in the visible region
  def visibleRegion(count: UInt): Bool = {
    val backPorchStart = sync
    val backPorchEnd = sync + backPorchStart
    val frontPorchStart = backPorchStart + visible
    (count >= backPorchEnd.U) && (count < frontPorchStart.U)
  }

  // Method that returns true if method is in the sync region
  def syncRegion(count: UInt): Bool = {
    count < sync.U
  }
}

// Full timing specification
case class VGATiming(horizontal: Timing, vertical: Timing)

// VGA Module
class VGA(timeSpec: VGATiming, pixelWidth: Int, memRdLatency: Int)
  extends Module {

  // Widths for the line and column counters
  val ptrHWidth: Int = log2Ceil(timeSpec.horizontal.total)
  val ptrVWidth: Int = log2Ceil(timeSpec.vertical.total)

  val io = IO(new Bundle {
    // Input pixel
    val pixelIn = Input(Color(pixelWidth))
    // Enable signal synchronous to system clock domain
    val en = Input(Bool())

    // Pointers for requesting pixels
    val rowPtr = Output(UInt(ptrVWidth.W))
    val colPtr = Output(UInt(ptrHWidth.W))
    // Read request signal (active high)
    val req = Output(Bool())
    // Output pixel synchronized to graphics clock
    val pixelOut = Output(Color(pixelWidth))
    // Synchronization pulses
    val hsync = Output(Bool())
    val vsync = Output(Bool())
  })


  // Row and column counters
  val rowCounter = RegInit(0.U(ptrVWidth.W))
  val colCounter = RegInit(0.U(ptrHWidth.W))
  // Output pixel
  val pixelOutReg = Reg(Color(pixelWidth))

  // Feed register through to outputs
  io.rowPtr := rowCounter
  io.colPtr := colCounter
  io.pixelOut := pixelOutReg

  when (io.en) {
    colCounter := colCounter + 1.U
    when (colCounter === (timeSpec.horizontal.total - 1).U) {
      rowCounter := rowCounter + 1.U
    }

    pixelOutReg := io.pixelIn
  }

  // Delayed registers that factor in memory read latency
  val rowCounterDly = Delay(UInt(ptrVWidth.W), 1, rowCounter)
  val colCounterDly = Delay(UInt(ptrHWidth.W), 1, colCounter)
  val reqDly = Delay(Bool(), 1, io.req)

  // Only request pixels if in the visible region
  io.req := timeSpec.vertical.visibleRegion(rowCounter) &
    timeSpec.horizontal.visibleRegion(colCounter)

  // If the there was no valid request, output a black pixel
  when (reqDly) {
    io.pixelOut := pixelOutReg
  } .otherwise {
    io.pixelOut.r := 0.U
    io.pixelOut.g := 0.U
    io.pixelOut.b := 0.U
  }

  /*************************/
  /* VSync and HSync logic */
  /*************************/

  // Register the sync pulses and use the counters, which are 1 cycle ahead
  val vsyncReg = Reg(Bool())
  val hsyncReg = Reg(Bool())

  vsyncReg := timeSpec.vertical.syncRegion(rowCounter)
  hsyncReg := timeSpec.horizontal.syncRegion(colCounter) &&
    timeSpec.vertical.visibleRegion(rowCounter)

  // Assign outputs
  io.vsync := vsyncReg
  io.hsync := hsyncReg
}

object GenVGA extends App {
  val pixelWidth: Int = 12
  val verticalTiming: Timing = Timing(480, 10, 10, 10)
  val horizontalTiming: Timing = Timing(640, 10, 10, 10)
  
  val videoTiming: VGATiming = VGATiming(verticalTiming, horizontalTiming)
  chisel3.Driver.execute(args, () => new VGA(videoTiming, pixelWidth, 1))
}
