`timescale 1ns/1ns
module tb_seriailzer;
  localparam CLK_HPERIOD = 4;
  localparam CLK_PERIOD = 2 * CLK_HPERIOD;

  localparam SCLK_HPERIOD = 1;
  localparam SCLK_PERIOD = 2 * SCLK_HPERIOD;

  localparam PKT_W = 4;

  reg clk = 1'b1;
  reg reset = 1'b1;

  reg [PKT_W - 1:0] pin;
  reg valid_in = 1'b0;

  reg sclk = 1'b1;
  reg sreset = 1'b1;

  wire sout; 


  Serializer dut(
    .clock(clk),
    .reset(reset),

    .io_sClk(sclk),
    .io_sReset(sreset),

    .io_pIn(pin),
    .io_validIn(valid_in),

    .io_sOut(sout)
  );
  
  initial begin: init_main
    $dumpfile("tb_serializer.vcd");
    // Dump everything
    $dumpvars();

    #CLK_PERIOD;
    reset = 1'b0;
    sreset = 1'b0;
    #(10 * CLK_PERIOD);

    pin = 'ha;
    valid_in = 1'b1;

    #CLK_PERIOD;
    pin = 'hf;

    #CLK_PERIOD;

    valid_in = 1'b0;

    #(14 * CLK_PERIOD);

    $finish();
  end

  always begin: clk_gen
    #CLK_HPERIOD;
    clk = ~clk;
  end

  always begin: sclk_gen
    #CLK_HPERIOD;
    sclk = ~sclk;
  end
endmodule
