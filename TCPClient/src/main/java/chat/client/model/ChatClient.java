package chat.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-side logic (the MODEL): socket communication, protocol parsing and the
 * typed commands ("allUsers", "end", "bye"). No JavaFX dependency.
 *
 * Protocol (one UTF-8 line per message, fields separated by '|'):
 *   client -> server : JOIN|username | MSG|text | USERS | BYE
 *   server -> client : INFO|text | CHAT|time|user|text | USERS|a,b,c | ERROR|text
 */
public class ChatClient {

    private final ChatClientListener listener;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Socket socket;
    private PrintWriter out;
    private boolean readOnly;

    public ChatClient(ChatClientListener listener) {
        this.listener = listener;
    }

    /** Connects and joins. A blank username means READ-ONLY MODE. */
    public void connect(String host, int port, String username) throws IOException {
        String name = username == null ? "" : username.replace("|", "").trim();
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 5000);
        socket = s;
        out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        readOnly = name.isEmpty();
        connected.set(true);
        out.println("JOIN|" + name);
        Thread.ofVirtual().name("client-reader").start(this::readLoop);
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Handles whatever the user typed: commands first, otherwise a chat message.
     * "allUsers" -> ask server for active users; "end"/"bye" -> disconnect.
     */
    public void submit(String input) {
        if (input == null) return;
        String text = input.replaceAll("\\s*\\R\\s*", " ").trim();
        if (text.isEmpty() || !connected.get()) return;

        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("bye")) {
            disconnect("You left the chat");
        } else if (text.equalsIgnoreCase("allUsers")) {
            send("USERS");
        } else if (readOnly) {
            listener.onSystem("READ-ONLY MODE: you cannot send messages.");
        } else {
            send("MSG|" + text);
        }
    }

    /** Notifies the server, closes the socket, fires onDisconnected once. */
    public void disconnect(String reason) {
        if (connected.compareAndSet(true, false)) {
            send("BYE");
            closeQuietly();
            listener.onDisconnected(reason);
        }
    }

    private void send(String line) {
        PrintWriter w = out;
        if (w != null) w.println(line);
    }

    private void readLoop() {
        String reason = "Connection to the server was lost";
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CHAT|")) {
                    String[] p = line.split("\\|", 4);
                    if (p.length == 4) listener.onMessage(new ChatMessage(p[1], p[2], p[3]));
                } else if (line.startsWith("INFO|")) {
                    listener.onSystem(line.substring(5));
                } else if (line.startsWith("USERS|")) {
                    String list = line.substring(6);
                    listener.onUsers(list.isEmpty() ? List.of() : Arrays.asList(list.split(",")));
                } else if (line.startsWith("ERROR|")) {
                    reason = line.substring(6);
                    break;
                }
            }
        } catch (IOException ignored) {
            // socket closed locally or dropped
        }
        if (connected.compareAndSet(true, false)) {
            closeQuietly();
            listener.onDisconnected(reason);
        }
    }

    private void closeQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) { }
    }
}
