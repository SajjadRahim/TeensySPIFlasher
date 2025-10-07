package spiflasher;

import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;
import picocli.CommandLine;

public class App {
    public static void main(String[] args) throws IOException {
        CommandLine cmd = new CommandLine(new Options.Main());

        try {
            CommandLine.ParseResult parseResult = cmd.parseArgs(args);

            // Check if help was requested
            if (parseResult.isUsageHelpRequested()) {
                cmd.usage(System.out);
                return;
            }

            // If no subcommand provided, print usage and exit
            if (!parseResult.hasSubcommand()) {
                cmd.usage(System.err);
                System.exit(1);
            }

            CommandLine.ParseResult subResult = parseResult.subcommand();

            // Check if help was requested for subcommand
            if (subResult.isUsageHelpRequested()) {
                subResult.commandSpec().commandLine().usage(System.out);
                return;
            }

            Object commandObject = subResult.commandSpec().userObject();
            Options.Main mainOptions = (Options.Main) parseResult.commandSpec().userObject();

            // process commands
            if (commandObject instanceof Options.Write opts) {
                validatePort(mainOptions.port, "write");
                writeChip(mainOptions.port, opts);
            } else if (commandObject instanceof Options.Read opts) {
                validatePort(mainOptions.port, "read");
                readChip(mainOptions.port, opts);
            } else if (commandObject instanceof Options.Verify opts) {
                validatePort(mainOptions.port, "verify");
                verifyChip(mainOptions.port, opts);
            } else if (commandObject instanceof Options.ListPorts opts) {
                listPorts(opts);
            } else if (commandObject instanceof Options.EraseChip opts) {
                validatePort(mainOptions.port, "erase-chip");
                eraseChip(mainOptions.port, opts);
            } else if (commandObject instanceof Options.Info opts) {
                validatePort(mainOptions.port, "info");
                getInfo(mainOptions.port, opts);
            } else {
                System.err.println("Unknown command type");
            }
        } catch (CommandLine.ParameterException e) {
            System.err.println("Error: " + e.getMessage());

            // TODO show usage if the subcommand wasn't recognized
            // System.err.println();
            // cmd.usage(System.err);

            System.exit(1);
        } catch (ReportableException e) {
            System.err.println("Error: " + e.getLocalizedMessage());
            System.exit(1);
        }
    }

    private static void validatePort(String port, String commandName) {
        if (port == null || port.isEmpty()) {
            throw new ReportableException(
                "Missing required global option: '--port=<port>'");
        }
    }

    public static void listPorts(Options.ListPorts options) throws IOException {
        if (options.verbose) {
            System.out.println("Available Serial Ports (verbose):\n");
            var ports = SerialPort.getCommPorts();
            for (int i = 0; i < ports.length; i++) {
                var port = ports[i];
                System.out.println("PortName: " + port.getSystemPortName());
                System.out.println("PortPath: " + port.getSystemPortPath());
                System.out.println("DescriptivePortName: " + port.getDescriptivePortName());
                System.out.println("PortDescription: " + port.getPortDescription());
                System.out.println("Manufacturer: " + port.getManufacturer());
                System.out.println("PortLocation: " + port.getPortLocation());
                System.out.println("ProductID: " + port.getProductID());
                System.out.println("VendorID: " + port.getVendorID());
                System.out.println();
            }
        } else {
            for (SerialPort port : SerialPort.getCommPorts()) {
                System.out.println(port.getSystemPortName());
            }
        }
    }

    public static void writeChip(String port, Options.Write options) throws IOException {
        java.nio.file.Path inputPath = java.nio.file.Path.of(options.file);
        if (!java.nio.file.Files.isRegularFile(inputPath)) {
            throw new ReportableException(
                "Input file does not exist or is not a regular file: %s".formatted(options.file));
        }

        long startTime = System.nanoTime();
        byte[] firmware = java.nio.file.Files.readAllBytes(inputPath);

        try (Client client = new Client(port)) {
            Client.Info info = client.getInfo();
            System.out.println(info.formatInfo());

            System.out.println("Writing...");
            client.write(info, firmware);

            if (options.verify) {
                System.out.println("\nVerifying...");
                client.verify(info, firmware);
            }

            System.out.println("\nDone. [%.2fs]".formatted((
                    System.nanoTime() - startTime) / 1_000_000_000.0));
        }
    }

    public static void readChip(String port, Options.Read options) throws IOException {
        java.nio.file.Path outputPath = java.nio.file.Path.of(options.file);

        // Check if file exists and --force wasn't specified
        if (java.nio.file.Files.exists(outputPath) && !options.force) {
            throw new ReportableException(
                "Output file already exists: %s; Use --force to overwrite"
                    .formatted(options.file));
        }

        long startTime = System.nanoTime();

        try (Client client = new Client(port)) {
            Client.Info info = client.getInfo();
            System.out.println(info.formatInfo());

            // Read the entire chip
            System.out.println("Reading...");
            byte[] data = client.read(info);

            // Verify
            if (options.verify) {
                System.out.println("\nVerifying...");
                client.verify(info, data);
            }

            // Write to file
            java.nio.file.Files.write(outputPath, data);

            System.out.println("\nDone. [%.2fs]".formatted((
                    System.nanoTime() - startTime) / 1_000_000_000.0));
        }
    }

    public static void verifyChip(String port, Options.Verify options) throws IOException {
        java.nio.file.Path inputPath = java.nio.file.Path.of(options.file);
        if (!java.nio.file.Files.isRegularFile(inputPath)) {
            throw new ReportableException(
                "Input file does not exist or is not a regular file: %s".formatted(options.file));
        }

        long startTime = System.nanoTime();
        byte[] firmware = java.nio.file.Files.readAllBytes(inputPath);

        try (Client client = new Client(port)) {
            Client.Info info = client.getInfo();
            System.out.println(info.formatInfo());

            System.out.println("Verifying...");
            client.verify(info, firmware);

            System.out.println("\nDone. [%.2fs]".formatted((
                    System.nanoTime() - startTime) / 1_000_000_000.0));
        }
    }

    public static void eraseChip(String port, Options.EraseChip options) throws IOException {
        long startTime = System.nanoTime();

        try (Client client = new Client(port)) {
            Client.Info info = client.getInfo();
            System.out.println(info.formatInfo());

            System.out.println("Erasing (this can take up to 5 minutes)...");

            client.eraseChip();

            System.out.println("\nDone. [%.2fs]".formatted((
                    System.nanoTime() - startTime) / 1_000_000_000.0));
        }
    }

    public static void getInfo(String port, Options.Info options) throws IOException {
        try (Client client = new Client(port)) {
            Client.Info info = client.getInfo();
            System.out.print(info.formatInfo());
        }
    }
}
