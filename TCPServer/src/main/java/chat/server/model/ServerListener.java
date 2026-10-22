package chat.server.model;

/**
 * Observer interface: the model reports events, the view decides how to show them.
 * Callbacks arrive on background (virtual) threads - a UI must hop to its own thread.
 */
public interface ServerListener {
    void onLog(String message);

    void onUserJoined(String username);

    void onUserLeft(String username);
}
