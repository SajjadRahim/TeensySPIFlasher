package spiflasher;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class Options {

    @Command(name = "list-ports", description = "List available serial ports")
    public static class ListPorts {
        @Option(names = {"-v", "--verbose"}, description = "Show detailed port information")
        public boolean verbose = false;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "read", description = "Read data from device")
    public static class Read {
        @Parameters(index = "0", description = "Output file")
        public String file;

        @Option(names = {"--force"}, description = "Overwrite file if it exists")
        public boolean force = false;

        @Option(names = {"--verify"},
            negatable = true,
            defaultValue = "true", fallbackValue = "true",
            description = "Verify after reading (default: ${DEFAULT-VALUE})")
        public boolean verify;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "write", description = "Write data to device")
    public static class Write {
        @Parameters(index = "0", description = "Input file")
        public String file;

        @Option(names = {"--verify"},
            negatable = true,
            defaultValue = "true", fallbackValue = "true",
            description = "Verify after writing (default: ${DEFAULT-VALUE})")
        public boolean verify;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "verify", description = "Verify data against device")
    public static class Verify {
        @Parameters(index = "0", description = "Input file")
        public String file;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "erase-chip", description = "Erase entire chip")
    public static class EraseChip {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "info", description = "Get device information")
    public static class Info {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;
    }

    @Command(name = "spi-flasher",
             description = "SPI Flasher Tool",
             subcommands = {ListPorts.class, Info.class, Read.class, Write.class, Verify.class, EraseChip.class})
    public static class Main {
        @Option(names = {"-p", "--port"}, description = "Serial port")
        public String port;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
        public boolean helpRequested = false;

        // Default behavior - will be handled by App class
    }
}
