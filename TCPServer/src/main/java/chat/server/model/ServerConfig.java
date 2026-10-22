package chat.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Network settings loaded at runtime from config.properties.
 * An external file next to the JAR (working directory) overrides the bundled default.
 * No JavaFX dependency.
 */
public record ServerConfig(String host, int port) {

    public static final String FILE_NAME = "config.properties";

    public static ServerConfig load() {
        Properties p = new Properties();
        // 1) bundled defaults
        try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in != null) p.load(in);
        } catch (IOException ignored) { }
        // 2) external override
        Path external = Path.of(FILE_NAME);
        if (Files.isRegularFile(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                p.load(in);
            } catch (IOException ignored) { }
        }
        String host = p.getProperty("server.ip", "0.0.0.0").trim();
        int port;
        try {
            port = Integer.parseInt(p.getProperty("server.port", "3000").trim());
        } catch (NumberFormatException e) {
            port = 3000;
        }
        return new ServerConfig(host, port);
    }
}
