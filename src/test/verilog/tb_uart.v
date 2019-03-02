`timescale 1ns/1ns
module tb_uart;
  localparam MAIN_CLK_HPERIOD = 1;
  localparam MAIN_CLK_PERIOD = 2 * MAIN_CLK_HPERIOD;
  localparam OTHER_CLK_HPERIOD = 3;
  localparam OTHER_CLK_PERIOD = 2 * OTHER_CLK_HPERIOD;

  localparam MAIN_RESET = 5;
  localparam OTHER_RESET = 5;

  localparam PKT_SIZE = 8;
  localparam RX_DEPTH = 32;
  localparam FIFO_DEPTH = 32;

  /* Signal for enabling the other clock, used to add a random phase offset
   * from the main clock */
  reg other_clk_en = 1;

  reg clk = 0;
  reg reset = 1;
  
  reg other_clk = 0;
  reg other_reset = 1;

  reg rx = 1'b1;
  wire tx;

  // Receiver IO
  wire [PKT_SIZE - 1:0] rx_req_pkt;
  reg rx_req = 0;
  wire rx_req_ready;
  wire rx_req_done;

  // Transmitter IO
  reg [PKT_SIZE - 1:0] tx_req_pkt;
  reg tx_req = 0;
  wire tx_req_ready;
  wire tx_req_done;

  // DUT instantiation
  UART dut(
    .clock(clk),
    .reset(reset),
    .io_otherClk(other_clk),
    .io_otherReset(other_reset),

    .io_rx(rx),
    .io_tx(tx),

    .io_rxReq_pkt(rx_req_pkt),
    .io_rxReq_req(rx_req),
    .io_rxReq_ready(rx_req_ready),
    .io_rxReq_done(rx_req_done),

    .io_txReq_pkt(tx_req_pkt),
    .io_txReq_req(tx_req),
    .io_txReq_ready(tx_req_ready),
    .io_txReq_done(tx_req_done)
  );

  // Sends the specified packet through the RX port
  task automatic inject_pkt;
    input [PKT_SIZE - 1:0] pkt;
    reg [PKT_SIZE - 1:0] pkt_shift;
    integer i;
  begin
    pkt_shift = pkt;
    // Mark
    rx = 0;
    #OTHER_CLK_PERIOD;
    repeat (PKT_SIZE) begin
      //  Transmit the MSb
      rx = pkt_shift[PKT_SIZE - 1];
      #OTHER_CLK_PERIOD;
      // Shift off the MSb
      pkt_shift = {pkt_shift[PKT_SIZE - 2:0], 1'b0};
    end

    // Stop bit
    rx = 1;
    #OTHER_CLK_PERIOD;
  end
  endtask

  // Writes a packet to the controller for transmission
  task transmit_pkt;
    input [PKT_SIZE - 1:0] pkt;
  begin
    tx_req_pkt = pkt;
    tx_req = 1'b1;
    #MAIN_CLK_PERIOD;
    tx_req = 1'b0;
  end
  endtask

  initial begin: init_main
    $dumpfile("tb_uart.vcd");
    $dumpvars();

    // Deassert reset
    fork
      begin
        repeat (MAIN_RESET) begin
          #MAIN_CLK_PERIOD;
        end
        reset = 0;

        repeat (2) begin
          wait (rx_req_ready);
          #MAIN_CLK_HPERIOD;
          rx_req = 1'b1;
          #MAIN_CLK_PERIOD;
          rx_req = 1'b0;
        end
      end
      begin
        repeat (OTHER_RESET) begin
          #OTHER_CLK_PERIOD;
        end
        other_reset = 0;

        inject_pkt(
          8'hcd
        );
        inject_pkt(
          8'haa
        );
      end
      begin
        #(4 * OTHER_CLK_PERIOD);
        transmit_pkt(
          8'h55
        );
      end
    join

    repeat (20) begin
      #MAIN_CLK_PERIOD;
    end

    wait (tx);
    wait (rx);
    $finish();
  end

  always begin: main_clk_gen
    #MAIN_CLK_HPERIOD;
    clk = ~clk;
  end

  always begin: other_clk_gen
    if (other_clk_en == 1) begin
      #OTHER_CLK_HPERIOD;
      other_clk = ~other_clk;
    end
  end

  // Random delay enable process
  integer en_delay;
  /*initial begin: other_clk_delay_en
    en_delay = $urandom_range(1, 10);
    #en_delay;
    other_clk_en = 1;
  end */

endmodule
