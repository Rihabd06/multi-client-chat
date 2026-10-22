package chat.server.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core server logic (the MODEL). Accepts TCP connections and hands each one to a
 * {@link ClientHandler} running on a virtual thread. Has NO dependency on JavaFX:
 * it only talks to the outside through {@link ServerListener}.
 */
public class ChatServer {

    private final ServerConfig config;
    private final ServerListener listener;
    private final List<ClientHandler> handlers = new CopyOnWriteArrayList<>();
    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(ServerConfig config, ServerListener listener) {
        this.config = config;
        this.listener = listener;
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(config.host(), config.port()));
        serverSocket = ss;
        running = true;
        log("Server Started on " + config.host() + ":" + config.port());
        Thread.ofVirtual().name("acceptor").start(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running) {
            try {
                log("Waiting for Client...");
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(this, socket);
                Thread.ofVirtual().name("client-" + socket.getPort()).start(handler);
            } catch (IOException e) {
                if (running) log("Accept error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) { }
        for (ClientHandler h : handlers) h.close();
        log("Server Stopped");
    }

    public boolean isRunning() {
        return running;
    }

    /** Registers a handler unless its (non-empty) username is already taken. */
    synchronized boolean admit(ClientHandler h) {
        if (!h.isReadOnly()) {
            for (ClientHandler other : handlers) {
                if (other.getUsername().equalsIgnoreCase(h.getUsername())) return false;
            }
        }
        handlers.add(h);
        return true;
    }

    void remove(ClientHandler h) {
        handlers.remove(h);
    }

    /** Sends a protocol line to every connected client (read-only guests included). */
    void broadcast(String line) {
        for (ClientHandler h : handlers) h.send(line);
    }

    /** Usernames of clients allowed to chat (read-only guests are not listed). */
    List<String> activeUsernames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler h : handlers) {
            if (!h.isReadOnly()) names.add(h.getUsername());
        }
        return names;
    }

    void log(String message) {
        listener.onLog(message);
    }

    void fireUserJoined(String u) {
        listener.onUserJoined(u);
    }

    void fireUserLeft(String u) {
        listener.onUserLeft(u);
    }
}
