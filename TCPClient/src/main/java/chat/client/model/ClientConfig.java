package chat.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Server address the client connects to. Defaults come from config.properties
 * (external file overrides the bundled one); command-line arguments override both.
 * No JavaFX dependency.
 */
public record ClientConfig(String host, int port) {

    public static final String FILE_NAME = "config.properties";

    public static ClientConfig load() {
        Properties p = new Properties();
        try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in != null) p.load(in);
        } catch (IOException ignored) { }
        Path external = Path.of(FILE_NAME);
        if (Files.isRegularFile(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                p.load(in);
            } catch (IOException ignored) { }
        }
        String host = p.getProperty("server.ip", "localhost").trim();
        int port;
        try {
            port = Integer.parseInt(p.getProperty("server.port", "3000").trim());
        } catch (NumberFormatException e) {
            port = 3000;
        }
        return new ClientConfig(host, port);
    }

    /**
     * Builds the config from command-line args: {@code <ServerIPAddress> <PortNumber>}.
     * Falls back to the properties file when no args are given.
     *
     * @throws IllegalArgumentException when the args are malformed
     */
    public static ClientConfig fromArgs(String[] args) {
        if (args == null || args.length == 0) return load();
        if (args.length != 2) throw new IllegalArgumentException("Expected exactly 2 arguments");
        try {
            int port = Integer.parseInt(args[1]);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return new ClientConfig(args[0], port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number: " + args[1]);
        }
    }
}
