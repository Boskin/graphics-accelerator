module SDPRAMBlackBox(
  rdClk,
  rdEn,
  rdAddr,
  rdData,

  wrClk,
  wrEn,
  wrAddr,
  wrData
);
  parameter DATA_W = 32;
  parameter DEPTH = 16;
  parameter INIT_FILE = "init.hex";

  localparam ADDR_W = $clog2(DEPTH);

  input rdClk;
  input rdEn;
  input [ADDR_W - 1:0] rdAddr;
  output reg [DATA_W - 1:0] rdData;

  input wrClk;
  input wrEn;
  input [ADDR_W - 1:0] wrAddr;
  input [DATA_W - 1:0] wrData;

  reg [DATA_W - 1:0] mem [DEPTH - 1:0];

  initial begin: init_mem
    $readmemh(INIT_FILE, mem, 0, DEPTH - 1);
  end

  always @(posedge rdClk) begin: rd_logic
    if (rdEn) begin
      rdData <= mem[rdAddr];
    end
  end

  always @(posedge wrClk) begin: wr_logic
    if (wrEn) begin
      mem[wrAddr] <= wrData;
    end
  end
endmodule
