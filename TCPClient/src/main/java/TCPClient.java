import chat.client.view.ClientApp;
import javafx.application.Application;

/**
 * Entry point: java TCPClient <ServerIPAddress> <PortNumber>   (e.g. java TCPClient localhost 3000)
 * Deliberately does NOT extend Application, so the shaded fat JAR starts without
 * the "JavaFX runtime components are missing" error.
 */
public class TCPClient {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
