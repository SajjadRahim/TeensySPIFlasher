package spiflasher;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Client implements AutoCloseable {
    private static final int VERSION_MAJOR = 0;
    private static final int VERSION_MINOR = 2;

    // Teensy commands
    private static final int CMD_SCRIPT_INFO = 0;
    private static final int CMD_SPI_INFO = 1;
    private static final int CMD_SPI_READ_BLOCK = 2;
    private static final int CMD_SPI_ERASE_CHIP = 3;
    private static final int CMD_SPI_ERASE_BLOCK = 4;
    private static final int CMD_SPI_WRITE_BLOCK = 5;

    // Response codes
    private static final int REQ_SUCCESS = 0;
    private static final int REQ_FAILURE = 1;
    private static final int REQ_CMD_NOT_RECOGNIZED = 2;
    private static final int REQ_ADDR_READ_TIMEOUT = 3;
    private static final int REQ_WRITE_PROTECTED = 4;
    private static final int REQ_CHIP_ERASE_FAILURE = 5;
    private static final int REQ_PAGE_READ_TIMEOUT = 6;
    private static final int REQ_PAGE_WRITE_FAILURE = 7;

    private final SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;

    public Client(String port) throws IOException {
        try {
            this.serialPort = SerialPort.getCommPort(port);
        } catch (SerialPortInvalidPortException e) {
            throw new ReportableException("Invalid serial port: " + port, e);
        }

        // Configure serial port settings to match Python code
        serialPort.setBaudRate(115200);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        // Immediately clear any existing data in the buffers upon opening the port
        serialPort.flushIOBuffers();

        // Open the port
        if (!serialPort.openPort()) {
            throw new ReportableException("Failed to open serial port: " + port);
        }

        // Block on read for up to 5 seconds
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 5000, 0);

        // Get input/output streams
        this.inputStream = serialPort.getInputStream();
        this.outputStream = serialPort.getOutputStream();

        // Verify we can communicate with the Teensy
        ping();
    }

    /**
     * Write a single byte to the serial port
     */
    private void writeByte(int value) throws IOException {
        outputStream.write(value & 0xFF);
        outputStream.flush();
    }

    /**
     * Read a single byte from the serial port
     */
    private int readByte() throws IOException {
        int value = inputStream.read();
        if (value == -1) {
            throw new ReportableException(
                    "Unexpected end of stream while reading from serial port");
        }
        return value;
    }

    /**
     * Read multiple bytes from the serial port
     */
    private byte[] readBytes(int count) throws IOException {
        byte[] buffer = new byte[count];
        int bytesRead = 0;
        while (bytesRead < count) {
            int result = inputStream.read(buffer, bytesRead, count - bytesRead);
            if (result == -1) {
                throw new ReportableException(
                        "Unexpected end of stream while reading from serial port");
            }
            bytesRead += result;
        }
        return buffer;
    }

    /**
     * Check response code and throw exception if not successful
     */
    private void checkResponseCode() throws IOException {
        int responseCode = readByte();

        if (responseCode == REQ_SUCCESS) {
            return;
        }

        String errorMessage;
        switch (responseCode) {
            case REQ_FAILURE -> errorMessage = "Unexpected failure";
            case REQ_CHIP_ERASE_FAILURE -> errorMessage = "Chip erase failed";
            case REQ_PAGE_WRITE_FAILURE -> errorMessage = "Page failed to write";
            case REQ_CMD_NOT_RECOGNIZED -> {
                int command = readByte();
                errorMessage = "Command not recognized: " + command;
            }
            case REQ_ADDR_READ_TIMEOUT -> errorMessage = """
                    Teensy timed out when receiving the address bytes. Did you send the \
                    correct number of bytes?";
                    """;
            case REQ_PAGE_READ_TIMEOUT -> errorMessage =
                    "Teensy timed out when receiving the block data from your PC.";
            case REQ_WRITE_PROTECTED -> errorMessage =
                    "Operation failed because NOR chip has write protection enabled.";
            default -> errorMessage =
                    "Received unknown error code: " + responseCode;
        }

        throw new RuntimeException(errorMessage);
    }

    /**
     * Record class to hold SPI chip information
     */
    public static record Info(
            int rdidManufacturer,
            int rdidMemoryType,
            int rdidCapacity,
            String manufacturerName,
            String chipType,
            int spiBlockCount,
            int spiSectorsPerBlock,
            int spiSectorSize,
            int spiAddressLength,
            boolean spiUse3ByteCmds) {

        public int spiBlockSize() {
            return spiSectorsPerBlock * spiSectorSize;
        }

        public long chipSizeBytes() {
            return (long) spiBlockSize() * spiBlockCount;
        }

        public long chipSizeKB() {
            return chipSizeBytes() / 1024;
        }

        public long chipSizeMB() {
            return chipSizeBytes() / (1024 * 1024);
        }

        public int totalSectors() {
            return spiSectorsPerBlock * spiBlockCount;
        }

        /**
         * Returns a formatted string with chip information for display
         */
        public String formatInfo() {
            StringBuilder sb = new StringBuilder();
            sb.append("SPI Information\n");
            sb.append("---------------\n");
            sb.append(String.format(
                    "Chip manufacturer: %s (0x%02x)\n",
                    manufacturerName, rdidManufacturer));
            sb.append(String.format(
                    "Chip type:         %s (0x%02x, 0x%02x)\n",
                    chipType, rdidMemoryType, rdidCapacity));

            // Format chip size (KB or MB based on size)
            if (chipSizeKB() <= 8192) {
                sb.append(String.format("Chip size:         %d KB\n", chipSizeKB()));
            } else {
                sb.append(String.format("Chip size:         %d MB\n", chipSizeMB()));
            }

            sb.append(String.format("Sector size:       %d bytes\n", spiSectorSize));
            sb.append(String.format("Block size:        %d bytes\n", spiBlockSize()));
            sb.append(String.format("Sectors per block: %d\n", spiSectorsPerBlock));
            sb.append(String.format("Number of blocks:  %d\n", spiBlockCount));
            sb.append(String.format("Number of sectors: %d\n", totalSectors()));

            return sb.toString();
        }
    }

    /**
     * Check if we are connected to the Teensy and verify that it is running the correct version
     */
    private void ping() throws IOException {
        writeByte(CMD_SCRIPT_INFO);

        int responseCode = readByte();
        int major = readByte();
        int minor = readByte();

        if (responseCode != REQ_SUCCESS) {
            throw new ReportableException("Ping failed with exit code " + responseCode);
        }

        if (major != VERSION_MAJOR || minor != VERSION_MINOR) {
            throw new ReportableException(String.format(
                "Ping failed (expected v%d.%02d, got v%d.%02d)",
                VERSION_MAJOR, VERSION_MINOR, major, minor));
        }
    }

    /**
     * Get SPI chip information
     */
    public Info getInfo() throws IOException {
        // Read SPI IDs
        writeByte(CMD_SPI_INFO);
        checkResponseCode();

        byte[] spiInfo = readBytes(3);
        int rdidManufacturer = spiInfo[0] & 0xFF;
        int rdidMemoryType = spiInfo[1] & 0xFF;
        int rdidCapacity = spiInfo[2] & 0xFF;

        // Determine chip configuration based on manufacturer and type
        String manufacturerName;
        String chipType;
        int blockCount;
        int sectorsPerBlock;
        int sectorSize;
        int addressLength;
        boolean use3ByteCmds;

        if (rdidManufacturer == 0xC2) {
            manufacturerName = "Macronix";
            if (rdidMemoryType == 0x20 && rdidCapacity == 0x19) {
                chipType = "MX25L25635F";
                blockCount = 512;
                sectorsPerBlock = 16;
                sectorSize = 0x1000;
                addressLength = 4;
                use3ByteCmds = false;
            } else {
                throw new ReportableException(String.format(
                        "Unknown Macronix chip type (0x%02x, 0x%02x)",
                        rdidMemoryType, rdidCapacity));
            }
        } else if (rdidManufacturer == 0x01) {
            manufacturerName = "Spansion/Cypress";
            if (rdidMemoryType == 0x60 && rdidCapacity == 0x19) {
                chipType = "S25FL256L";
                blockCount = 512;
                sectorsPerBlock = 16;
                sectorSize = 0x1000;
                addressLength = 4;
                use3ByteCmds = false;
            } else {
                throw new ReportableException(String.format(
                        "Unknown Spansion/Cypress chip type (0x%02x, 0x%02x)",
                        rdidMemoryType, rdidCapacity));
            }
        } else {
            throw new ReportableException(String.format(
                    "Unknown chip manufacturer (0x%02x)",
                    rdidManufacturer));
        }

        return new Info(
            rdidManufacturer,
            rdidMemoryType,
            rdidCapacity,
            manufacturerName,
            chipType,
            blockCount,
            sectorsPerBlock,
            sectorSize,
            addressLength,
            use3ByteCmds
        );
    }

    /**
     * Close the serial connection
     */
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                // Ignore close errors
            }
            serialPort.closePort();
        }
    }
}