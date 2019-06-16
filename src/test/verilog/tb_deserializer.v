`timescale 1ns/1ns
module tb_deserializer;
  localparam SCLK_HPERIOD = 1;
  localparam SCLK_PERIOD = 2 * SCLK_HPERIOD;

  localparam PCLK_HPERIOD = 10;
  localparam PCLK_PERIOD = 2 * PCLK_HPERIOD;

  localparam TEST_COUNT = 100;
  localparam PKT_W = 8;

  reg sclk = 1'b0;
  reg sreset = 1'b1;

  reg sin = 1'b1;

  reg pclk = 1'b0;
  reg preset = 1'b1;

  reg rd_en = 1'b0;

  wire data_ready;
  wire [PKT_W - 1:0] pout;
  wire valid_out;


  /*********************/
  /* DUT instantiation */
  /*********************/
  Deserializer dut(
    .io_sClk(sclk),
    .io_sReset(sreset),
    .io_sIn(sin),

    .clock(pclk),
    .reset(preset),
    .io_rdEn(rd_en),
    .io_dataReady(data_ready),
    .io_pOut(pout),
    .io_validOut(valid_out)
  );


  /****************/
  /* Helper tasks */
  /****************/
  task serial_send;
    input [PKT_W - 1:0] pkt;
    reg [PKT_W - 1:0] pkt_rot;
  begin
    // Start sequence
    sin = 1'b0;
    #SCLK_PERIOD;
    pkt_rot = pkt;
    repeat (PKT_W) begin
      sin = pkt_rot[PKT_W - 1];
      #SCLK_PERIOD;
      // Rotate packet
      pkt_rot = pkt_rot << 1;
    end
    // Stop sequence
    sin = 1'b1;
    #SCLK_PERIOD;
  end
  endtask


  /*******************/
  /* Simulation main */
  /*******************/
  initial begin: init_main
    integer i;
    integer j;
    reg [PKT_W - 1:0] test_pkt [TEST_COUNT - 1:0];

    $dumpfile("tb_deserializer.vcd");
    $dumpvars();

    for (i = 0; i < TEST_COUNT; i = i + 1) begin
      test_pkt[i] = $urandom % (2 ** PKT_W - 1);
    end
    test_pkt[0] = 'hff;


    fork
      begin: slogic
        #SCLK_PERIOD;
        sreset = 1'b0;

        for (i = 0; i < TEST_COUNT; i = i + 1) begin
          serial_send(
            test_pkt[i]
          );
        end
      end

      begin: plogic
        #PCLK_PERIOD;
        preset = 1'b0;
        #PCLK_PERIOD;

        for (j = 0; j < TEST_COUNT; j = j + 1) begin
          wait (data_ready);
          #PCLK_HPERIOD;

          rd_en = 1'b1;
          #PCLK_PERIOD;
          rd_en = 1'b0;

          if (test_pkt[j] != pout) begin
            $display("Packet %0d mismatch! RES: %x; REF: %x", j, pout,
              test_pkt[j]);
          end
          #PCLK_PERIOD;
        end
      end
    join

    $finish();
  end

  always begin: sclk_gen
    #SCLK_HPERIOD;
    sclk = ~sclk;
  end

  always begin: pclk_gen
    #PCLK_HPERIOD;
    pclk = ~pclk;
  end
endmodule
