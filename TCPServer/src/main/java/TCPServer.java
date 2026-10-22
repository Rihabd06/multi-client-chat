import chat.server.view.ServerApp;
import javafx.application.Application;

/**
 * Entry point: java TCPServer   (or: java -jar TCPServer.jar)
 * Deliberately does NOT extend Application, so the shaded fat JAR starts without
 * the "JavaFX runtime components are missing" error.
 */
public class TCPServer {
    public static void main(String[] args) {
        Application.launch(ServerApp.class, args);
    }
}
