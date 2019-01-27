package com.boskin.communications.uart

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util._

import com.boskin.synchronization.{AsyncFIFOReq, CDC}

class TransmitSubsystemIO(val pktSize: Int) extends Bundle {
  val txReq = new UARTReq(pktSize, true)
  val fifoRdReq = Flipped(new AsyncFIFOReq(Bool(), false))
  val fifoWrReq = Flipped(new AsyncFIFOReq(Bool(), true))
  val fifoEmpty = Input(Bool())
  val tx = Output(Bool())
  val otherClk = Input(Clock())
  val otherReset = Input(Bool())
}

class TransmitSubsystem(pktSize: Int) extends Module {
  val bitCountWidth = log2Ceil(pktSize)
  val io = IO(new TransmitSubsystemIO(pktSize))

  /* States for transmit FSM:
   * idle: waiting for transmit request
   * transData: shifting data into transmit FIFO */
  val idle :: transData :: Nil = Enum(2)
  val state = RegInit(idle)

  /* Register that keeps count of how many bits have been shifted into the
   * transmit FIFO */
  val bitCount = Reg(UInt(bitCountWidth.W))

  // Shift register that holds a request packet
  val pktShift = Reg(UInt((pktSize - 1).W))
  // Write-enable for transmit FIFO
  val wrEn = RegInit(false.B)
  io.fifoWrReq.en := wrEn
  // Write data for transmit FIFO
  val wrData = Reg(Bool())
  io.fifoWrReq.data := wrData

  // Done response signal
  // Done is pulsed the cycle the last bit is shifted into the FIFO
  io.txReq.done := RegNext((state === transData) &&
    (bitCount === (pktSize - 1).U))

  // Indicate that the core is ready if the state is idle
  io.txReq.ready := state === idle

  // TX FIFO write fsm
  switch (state) {
    is (idle) {
      // Do nothing until a TX write request comes
      when (io.txReq.req) {
        // Load the shift register with all but the MSb of the packet
        pktShift := io.txReq.pkt(pktSize - 2, 0)
        // Write to the FIFO next cycle
        wrEn := true.B
        // Use the input instead of the register for the first cycle
        wrData := io.txReq.pkt(pktSize - 1)

        // Start the bit count
        bitCount := 0.U
        // Transition state
        state := transData
      }
    }

    is (transData) {
      // Check if all of the bits have been serialized
      when (bitCount === (pktSize - 1).U) {
        // If they have, stop writing and go back to idle
        wrEn := false.B

        state := idle
      } .otherwise {
        // Otherwise, shift a bit off the register and write
        pktShift := Cat(pktShift(pktSize - 3, 0), 0.U(1.W))
        wrData := pktShift(pktSize - 2)

        // Increment the number of bits written
        bitCount := bitCount + 1.U
      }
    }
  }

  // TX driver/TX FIFO read logic
  withClockAndReset (io.otherClk, io.otherReset) {
    // Will only work as long as state is 1 bit
    val stateSync = CDC(state, UInt(1.W))
    // Used for stateSync edge detection
    val stateSyncDelay = RegNext(stateSync)

    io.tx := true.B
    io.fifoRdReq.en := false.B

    when (stateSync === transData && stateSyncDelay === idle) {
      io.tx := false.B
      io.fifoRdReq.en := true.B
    // Output bits until the FIFO is empty
    } .elsewhen (!io.fifoEmpty) {
      io.tx := io.fifoRdReq.data
      io.fifoRdReq.en := true.B
    }
  }
}
