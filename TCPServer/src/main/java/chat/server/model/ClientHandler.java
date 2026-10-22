package chat.server.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Serves ONE client connection. Runs on its own virtual thread.
 *
 * Wire protocol (one UTF-8 line per message, fields separated by '|'):
 *   client -> server : JOIN|username (empty username = read-only) | MSG|text | USERS | BYE
 *   server -> client : INFO|text | CHAT|time|user|text | USERS|a,b,c | ERROR|text
 */
public class ClientHandler implements Runnable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ChatServer server;
    private final Socket socket;
    private PrintWriter out;
    private volatile String username = "";

    ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public boolean isReadOnly() {
        return username.isEmpty();
    }

    /** Thread-safe: several threads may broadcast to this client at once. */
    public synchronized void send(String line) {
        if (out != null) out.println(line);
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) { }
    }

    @Override
    public void run() {
        boolean admitted = false;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String first = in.readLine();
            if (first == null || !first.startsWith("JOIN|")) return;
            username = first.substring(5).replace("|", "").trim();

            if (!server.admit(this)) {
                send("ERROR|Username '" + username + "' is already in use");
                return;
            }
            admitted = true;

            if (isReadOnly()) {
                server.log("Welcome guest (READ-ONLY MODE) from " + socket.getRemoteSocketAddress());
                send("INFO|You are in READ-ONLY MODE. Reconnect with a username to send messages.");
            } else {
                server.log("Welcome " + username + " (" + socket.getRemoteSocketAddress() + ")");
                send("INFO|Welcome " + username + "!");
                server.broadcast("INFO|" + username + " joined the chat");
                server.fireUserJoined(username);
            }

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("MSG|")) {
                    if (isReadOnly()) {
                        send("INFO|READ-ONLY MODE: you cannot send messages.");
                    } else {
                        String text = line.substring(4).trim();
                        if (!text.isEmpty()) {
                            server.broadcast("CHAT|" + LocalTime.now().format(TIME) + "|" + username + "|" + text);
                        }
                    }
                } else if (line.equals("USERS")) {
                    send("USERS|" + String.join(",", server.activeUsernames()));
                } else if (line.equals("BYE")) {
                    break;
                }
            }
        } catch (IOException e) {
            // connection dropped abruptly - handled in finally
        } finally {
            if (admitted) {
                server.remove(this);
                if (isReadOnly()) {
                    server.log("Guest (read-only) disconnected");
                } else {
                    server.log(username + " left the chat");
                    server.broadcast("INFO|" + username + " left the chat");
                    server.fireUserLeft(username);
                }
            }
            close();
        }
    }
}
