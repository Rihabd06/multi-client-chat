package chat.client.model;

import java.util.List;

/**
 * Observer interface implemented by the view. Callbacks arrive on a background
 * (virtual) thread - a UI must hop to its own thread before touching controls.
 */
public interface ChatClientListener {
    void onMessage(ChatMessage message);

    /** Info text from the server or the client itself (welcome, joins, warnings...). */
    void onSystem(String text);

    void onUsers(List<String> users);

    /** Fired exactly once when the connection ends (user left, server gone, rejected). */
    void onDisconnected(String reason);
}
