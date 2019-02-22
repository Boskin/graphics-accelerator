/* Simple Verilog testbench for testing the asynchronous FIFO module; it was
 * easier to generate 2 asynchronous clocks in Verilog rather than in Chisel's
 * testing environment */
`timescale 1ns/1ns
module tb_async_fifo;
  localparam RD_CLK_HPERIOD = 1;
  localparam RD_CLK_PERIOD = 2 * RD_CLK_HPERIOD;
  localparam RD_RESET = 3;

  localparam WR_CLK_HPERIOD = 3;
  localparam WR_CLK_PERIOD = 2 * WR_CLK_HPERIOD;
  localparam WR_RESET = 3;

  localparam TEST_COUNT = 17;

  // Not adjustable unless the AsyncFIFO is regenerated
  localparam DATA_W = 4;
  localparam FIFO_DEPTH = 16;

  // Read side IO
  reg rd_clk = 0;
  reg rd_reset = 1;
  reg rd_en = 0;
  wire [DATA_W - 1:0] rd_data;
  wire rd_valid;
  wire empty;

  // Write side IO
  reg wr_clk = 0;
  reg wr_reset = 1;
  reg wr_en = 0;
  reg [DATA_W - 1:0] wr_data;
  wire wr_valid;
  wire full;

  /*********************/
  /* DUT instantiation */
  /*********************/
  AsyncFIFO dut(
    .clock(1'b0),
    .reset(1'b0),

    .io_rdClk(rd_clk),
    .io_rdReset(rd_reset),
    .io_rdReq_en(rd_en),
    .io_rdReq_data(rd_data),
    .io_rdReq_valid(rd_valid),
    .io_empty(empty),

    .io_wrClk(wr_clk),
    .io_wrReset(wr_reset),
    .io_wrReq_en(wr_en),
    .io_wrReq_data(wr_data),
    .io_wrReq_valid(wr_valid),
    .io_full(full)
  );

  integer ri;
  integer wi;
  initial begin: init_main
    $dumpfile("tb_async_fifo.vcd");
    $dumpvars();

    fork
      begin: rd_logic
        repeat (RD_RESET) begin
          #RD_CLK_PERIOD;
        end
        rd_reset = 0;
        for (ri = 0; ri < TEST_COUNT; ri = ri + 1) begin
          wait (empty == 0);
          #RD_CLK_HPERIOD;

          rd_en = 1;
          #RD_CLK_PERIOD;

          rd_en = 0;
        end
      end
      begin: wr_logic
        repeat (WR_RESET) begin
          #WR_CLK_PERIOD;
        end
        wr_reset = 0;

        wr_en = 1;
        for (wi = 0; wi < TEST_COUNT; wi = wi + 1) begin
          wr_data = $urandom_range(0, 2 ** DATA_W - 1);
          #WR_CLK_PERIOD;
        end
        wr_en = 0;
      end
    join
    $finish();
  end

  always begin: rd_clk_gen
    #RD_CLK_HPERIOD;
    rd_clk = ~rd_clk;
  end

  always begin: wr_clk_gen
    #WR_CLK_HPERIOD;
    wr_clk = ~wr_clk;
  end
endmodule
