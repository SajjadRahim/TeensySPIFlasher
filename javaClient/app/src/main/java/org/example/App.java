package org.example;

import com.fazecast.jSerialComm.SerialPort;
import picocli.CommandLine;

public class App {
    public static void main(String[] args) {
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

            // process commands
            if (commandObject instanceof Options.Write opts) {
                writeFirmware(opts);
            } else if (commandObject instanceof Options.Dump opts) {
                dumpData(opts);
            } else if (commandObject instanceof Options.ListPorts opts) {
                listPorts(opts);
            } else if (commandObject instanceof Options.EraseChip opts) {
                eraseChip(opts);
            } else if (commandObject instanceof Options.Info opts) {
                getInfo(opts);
            } else {
                System.err.println("Unknown command type");
            }
        } catch (CommandLine.ParameterException e) {
            System.err.println("Error: " + e.getMessage());

            // TODO show usage if the subcommand wasn't recognized
            // System.err.println();
            // cmd.usage(System.err);

            System.exit(1);
        }
    }

    public static void listPorts(Options.ListPorts options) {
        if (options.verbose) {
            System.out.println("Available Serial Ports (verbose):\n");
            var ports = SerialPort.getCommPorts();
            for (int i = 0; i < ports.length; i++) {
                var port = ports[i];
                System.out.println("SystemPortPath: " + port.getSystemPortPath());
                System.out.println("PortName: " + port.getSystemPortName());
                System.out.println("DescriptivePortName: " + port.getDescriptivePortName());
                System.out.println("PortDescription: " + port.getPortDescription());
                System.out.println("Manufacturer: " + port.getManufacturer());
                System.out.println("PortLocation: " + port.getPortLocation());
                System.out.println("ProductID: " + port.getProductID());
                System.out.println("VendorID: " + port.getVendorID());
                System.out.println();
            }
        } else {
            System.out.println("Available Serial Ports:\n");
            var ports = SerialPort.getCommPorts();
            for (int i = 0; i < ports.length; i++) {
                var port = ports[i];
                System.out.println(i + ": " + port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")");
            }
        }
    }

    public static void writeFirmware(Options.Write options) {
        System.out.println("Writing firmware from " + options.inputFile +
                          " verify = " + options.verify +
                          " to port " + options.port +
                          " offset " + options.offset +
                          " at offset " + options.offset + " length " + options.length);
        if (options.verify) {
            System.out.println("Will verify after writing");
        }
        // TODO: Implement
    }

    public static void dumpData(Options.Dump options) {
        System.out.println("Reading from port " + options.port +
                          " to file " + options.outputFile +
                          " at offset " + options.offset + " length " + options.length);
        // TODO: Implement
    }

    public static void eraseChip(Options.EraseChip options) {
        System.out.println("Erasing entire chip on port " + options.port);
        // TODO: Implement
    }

    public static void getInfo(Options.Info options) {
        try (Client client = new Client(options.port)) {
            Client.Info info = client.getInfo();
            System.out.print(info.formatInfo());
        } catch (Client.ClientException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
