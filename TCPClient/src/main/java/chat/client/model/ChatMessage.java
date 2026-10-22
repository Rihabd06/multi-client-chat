package chat.client.model;

/** One chat line as formatted by the server (sender + server time + text). */
public record ChatMessage(String time, String sender, String text) { }
