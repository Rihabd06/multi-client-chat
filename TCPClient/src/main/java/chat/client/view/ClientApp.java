package chat.client.view;

import chat.client.model.ChatClient;
import chat.client.model.ChatClientListener;
import chat.client.model.ChatMessage;
import chat.client.model.ClientConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

/** JavaFX VIEW for the client. All networking lives in {@link ChatClient} (the model). */
public class ClientApp extends Application implements ChatClientListener {

    private ClientConfig config;
    private ChatClient client;

    private final StackPane root = new StackPane();

    // login screen
    private final TextField usernameField = new TextField();
    private final Label loginMessage = new Label();

    // chat screen
    private final Label statusLabel = new Label("Online");
    private final Circle statusDot = new Circle(7, Color.web("#1b8a3a"));
    private final Label identityLabel = new Label();
    private final Label modeLabel = new Label();
    private final TextArea messages = new TextArea();
    private final TextArea input = new TextArea();
    private final Button sendButton = new Button("SEND");
    private final Button leaveButton = new Button("LEAVE");

    private GridPane loginPane;
    private GridPane chatPane;

    @Override
    public void start(Stage stage) {
        try {
            config = ClientConfig.fromArgs(getParameters().getRaw().toArray(new String[0]));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: java TCPClient <ServerIPAddress> <PortNumber>");
            Platform.exit();
            return;
        }

        loginPane = buildLoginPane();
        chatPane = buildChatPane();
        root.getChildren().add(loginPane);

        Scene scene = new Scene(root, 640, 560);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("TCPClient - Group Chat");
        stage.setScene(scene);
        stage.show();
        usernameField.requestFocus();
    }

    @Override
    public void stop() {
        if (client != null) client.disconnect("Application closed");
    }

    // ------------------------------------------------------------------ login

    private GridPane buildLoginPane() {
        Label title = new Label("Join the Group Chat");
        title.getStyleClass().add("title");
        Label server = new Label("Server: " + config.host() + ":" + config.port());
        server.getStyleClass().add("hint");

        Label nameLabel = new Label("Username");
        usernameField.setPromptText("Enter a username (leave empty for read-only)");
        usernameField.setOnAction(e -> connect());

        Label hint = new Label("No username = READ-ONLY MODE (you can read but not send).");
        hint.getStyleClass().add("hint");

        Button connectButton = new Button("CONNECT");
        connectButton.getStyleClass().add("btn-connect");
        connectButton.setMaxWidth(Double.MAX_VALUE);
        connectButton.setOnAction(e -> connect());

        loginMessage.getStyleClass().add("error");
        loginMessage.setWrapText(true);

        GridPane g = new GridPane();
        g.setPadding(new Insets(40));
        g.setVgap(12);
        g.setHgap(10);
        g.setAlignment(Pos.CENTER);
        g.add(title, 0, 0, 2, 1);
        g.add(server, 0, 1, 2, 1);
        g.add(nameLabel, 0, 2);
        g.add(usernameField, 1, 2);
        g.add(hint, 0, 3, 2, 1);
        g.add(connectButton, 0, 4, 2, 1);
        g.add(loginMessage, 0, 5, 2, 1);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(320);
        g.getColumnConstraints().addAll(new ColumnConstraints(), c1);
        return g;
    }

    private void connect() {
        loginMessage.setText("");
        String username = usernameField.getText().trim();
        client = new ChatClient(this);
        try {
            client.connect(config.host(), config.port(), username);
        } catch (Exception e) {
            loginMessage.setText("Could not connect to " + config.host() + ":" + config.port()
                    + " (" + e.getMessage() + ")");
            return;
        }
        boolean readOnly = client.isReadOnly();
        messages.clear();
        identityLabel.setText(readOnly ? "Guest" : "User: " + username);
        modeLabel.setText(readOnly ? "READ-ONLY MODE" : "");
        input.setDisable(readOnly);
        sendButton.setDisable(readOnly);
        input.setPromptText(readOnly ? "READ-ONLY MODE - reconnect with a username to chat"
                : "Type a message. Enter = send. Commands: allUsers, end, bye");
        setOnline(true);
        root.getChildren().setAll(chatPane);
        if (!readOnly) input.requestFocus();
    }

    // ------------------------------------------------------------------- chat

    private GridPane buildChatPane() {
        statusLabel.getStyleClass().add("online");
        identityLabel.getStyleClass().add("section-label");
        modeLabel.getStyleClass().add("readonly");

        HBox header = new HBox(8, statusDot, statusLabel, identityLabel, modeLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        messages.setEditable(false);
        messages.setWrapText(true);

        input.setWrapText(true);
        input.setPrefRowCount(3);
        // Enter sends, Shift+Enter is swallowed (messages are single-line)
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                sendInput();
            }
        });

        sendButton.getStyleClass().add("btn-send");
        sendButton.setMaxWidth(Double.MAX_VALUE);
        sendButton.setOnAction(e -> sendInput());
        leaveButton.getStyleClass().add("btn-leave");
        leaveButton.setMaxWidth(Double.MAX_VALUE);
        leaveButton.setOnAction(e -> client.disconnect("You left the chat"));
        VBox buttons = new VBox(8, sendButton, leaveButton);
        buttons.setPrefWidth(110);

        GridPane g = new GridPane();
        g.setPadding(new Insets(16));
        g.setHgap(10);
        g.setVgap(10);
        g.add(header, 0, 0, 2, 1);
        g.add(messages, 0, 1, 2, 1);
        g.add(input, 0, 2);
        g.add(buttons, 1, 2);
        GridPane.setHalignment(buttons, HPos.RIGHT);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, new ColumnConstraints());
        RowConstraints r1 = new RowConstraints();
        r1.setVgrow(Priority.ALWAYS);
        g.getRowConstraints().addAll(new RowConstraints(), r1, new RowConstraints());
        return g;
    }

    private void sendInput() {
        String text = input.getText();
        input.clear();
        if (client != null) client.submit(text);
    }

    private void setOnline(boolean online) {
        statusLabel.setText(online ? "Online" : "Offline");
        statusLabel.getStyleClass().removeAll("online", "offline");
        statusLabel.getStyleClass().add(online ? "online" : "offline");
        statusDot.setFill(Color.web(online ? "#1b8a3a" : "#c62828"));
    }

    // ------------------------------------- ChatClientListener (background threads)

    @Override
    public void onMessage(ChatMessage m) {
        Platform.runLater(() -> messages.appendText("[" + m.time() + "] " + m.sender() + ": " + m.text() + "\n"));
    }

    @Override
    public void onSystem(String text) {
        Platform.runLater(() -> messages.appendText("*** " + text + "\n"));
    }

    @Override
    public void onUsers(List<String> users) {
        Platform.runLater(() -> messages.appendText("*** Active users (" + users.size() + "): "
                + String.join(", ", users) + "\n"));
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> {
            setOnline(false);
            loginMessage.setText(reason);
            root.getChildren().setAll(loginPane);
            usernameField.requestFocus();
        });
    }
}
