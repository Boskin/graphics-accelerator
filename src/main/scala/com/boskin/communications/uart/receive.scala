package com.boskin.communications.uart

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util._

import com.boskin.synchronization.{AsyncFIFOReq, ShiftRegister}
import com.boskin.memory.FIFO

class ReceiveSubsystemIO(pktSize: Int) extends Bundle {
  val rxReq = new UARTReq(pktSize, false)
  val fifoRdReq = Flipped(new AsyncFIFOReq(Bool(), false))
  val fifoEmpty = Input(Bool())
  val fifoWrReq = Flipped(new AsyncFIFOReq(Bool(), true))
  val rxSync = Input(Bool())
  val otherClk = Input(Clock())
  val otherReset = Input(Bool())

  override def cloneType: this.type = {
    new ReceiveSubsystemIO(pktSize).asInstanceOf[this.type]
  }
}

class ReceiveSubsystem(pktSize: Int, pktBufDepth: Int) extends Module {
  val bitCountWidth = log2Ceil(pktSize)

  val io = IO(new ReceiveSubsystemIO(pktSize))


  /*****************/
  /* RX FIFO logic */
  /*****************/
  withClockAndReset (io.otherClk, io.otherReset) {
    /* RX FIFO control FSM:
     * idle: wait for RX to go low (indicating the start of a data transfer)
     * recvData: count and read the bits being input */
    val idle :: recvData :: Nil = Enum(2)
    val state = RegInit(idle)

    // Counts how many bits have been read in the current transaction
    val bitCount = RegInit(0.U(bitCountWidth.W))

    // RX FIFO write enable
    val wrEn = RegInit(false.B)

    // RX FIFO write enable (set in RX FIFO write FSM)
    io.fifoWrReq.en := wrEn
    io.fifoWrReq.data := io.rxSync

    switch (state) {
      is (idle) {
        // Wait for start bit
        when (!io.rxSync) {
          state := recvData
          bitCount := 0.U
          wrEn := true.B
        }
      }
      is (recvData) {
        /* If the number of bits in a packet has been received, end the
         * transaction */
        when (bitCount === (pktSize - 1).U) {
          state := idle
          wrEn := false.B
        } .otherwise {
          bitCount := bitCount + 1.U
        }
      }
    }
  }


  // Shift register that buffers the bits in the RX FIFO to form a packet
  val pktShiftRegInst = Module(new ShiftRegister(Bool(), pktSize))
  /* Counter that counts how many bits have been shifted into the shift
   * register */
  val bitCount = RegInit(0.U(bitCountWidth.W))

  val shiftRegEn = Wire(Bool())
  val fifoRdEn = Wire(Bool())

  pktShiftRegInst.io.din := io.fifoRdReq.data
  pktShiftRegInst.io.en := shiftRegEn

  // FIFO that holds full UART packets
  val pktFIFOInst = Module(new FIFO(UInt(pktSize.W), pktBufDepth))
  // Write enable (FSM output)
  val pktFIFOWrEn = RegInit(false.B)


  /* If there are bits in the RX FIFO, read them and shift them into the shift
   * register */
  shiftRegEn := !io.fifoEmpty
  fifoRdEn := !io.fifoEmpty

  // Count how many bits have been read from the FIFO
  when (!io.fifoEmpty) {
    when (bitCount === (pktSize - 1).U) {
      bitCount := 0.U
      pktFIFOWrEn := true.B
    } .otherwise {
      bitCount := bitCount + 1.U
      pktFIFOWrEn := false.B
    }
  }

  /*****************************/
  /* RX request bundle drivers */
  /*****************************/
  /* Simply hook up the RX FIFO's read port to the data read port of the
   * request */
  io.rxReq.pkt := io.fifoRdReq.data
  // Indicate the receiver is ready if there is a packet available to be read
  io.rxReq.ready := !pktFIFOInst.io.empty
  // Indicate that the operation is completed if a full packet was read
  io.rxReq.done := pktFIFOInst.io.rdReq.valid


  /***********************/
  /* Packet FIFO drivers */
  /***********************/
  // Hook up the write port to the shift register
  pktFIFOInst.io.wrReq.data := pktShiftRegInst.io.dout
  // Write enable (determined by number of bits shifted into shift register)
  pktFIFOInst.io.wrReq.en := pktFIFOWrEn
  pktFIFOInst.io.rdReq.en := io.rxReq.req


  /********************/
  /* Bit FIFO drivers */
  /********************/
  // Read bits from the RX FIFO
  io.fifoRdReq.en := fifoRdEn
}
