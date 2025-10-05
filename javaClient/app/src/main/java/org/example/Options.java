package org.example;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public class Options {

    @Command(name = "list-ports", description = "List available serial ports")
    public static class ListPorts {
        @Option(names = {"-v", "--verbose"}, description = "Show detailed port information")
        public boolean verbose = false;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "dump", description = "Read data from device")
    public static class Dump {
        @Option(names = {"-p", "--port"}, description = "Serial port", required = true)
        public String port;

        @Option(names = {"-o", "--output"}, description = "Output file", required = true)
        public String outputFile;

        @Option(names = {"--offset"}, description = "Offset to read in blocks", defaultValue = "0")
        public long offset;

        @Option(names = {"--length"}, description = "Length to read in blocks")
        public Long length;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "write", description = "Write data to device")
    public static class Write {
        @Option(names = {"-p", "--port"}, description = "Serial port", required = true)
        public String port;

        @Option(names = {"-i", "--input"}, description = "Input file", required = true)
        public String inputFile;

        @Option(names = {"--offset"}, description = "Offset to write in blocks", defaultValue = "0")
        public long offset;

        @Option(names = {"--length"}, description = "Length to write in blocks")
        public Long length;

        @Option(names = {"-v", "--verify"},
            negatable = true,
            defaultValue = "true", fallbackValue = "true",
            description = "Verify after writing (default: ${DEFAULT-VALUE})")
        public boolean verify;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "erase-chip", description = "Erase entire chip")
    public static class EraseChip {
        @Option(names = {"-p", "--port"}, description = "Serial port", required = true)
        public String port;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "info", description = "Get device information")
    public static class Info {
        @Option(names = {"-p", "--port"}, description = "Serial port", required = true)
        public String port;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "TeensySPIFlasher",
             description = "Teensy SPI Flasher Tool",
             subcommands = {ListPorts.class, Info.class, Dump.class, Write.class, EraseChip.class})
    public static class Main {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;

        // Default behavior - will be handled by App class
    }
}
