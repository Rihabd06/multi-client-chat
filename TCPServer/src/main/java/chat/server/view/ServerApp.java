package chat.server.view;

import chat.server.model.ChatServer;
import chat.server.model.ServerConfig;
import chat.server.model.ServerListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** JavaFX VIEW for the server. Contains no networking code - it only listens to the model. */
public class ServerApp extends Application implements ServerListener {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> users = FXCollections.observableArrayList();
    private final Map<String, String> colors = new HashMap<>();
    private final Random random = new Random();
    private final TextArea logArea = new TextArea();
    private final Label statusLabel = new Label("Starting...");
    private ChatServer server;

    @Override
    public void start(Stage stage) {
        ServerConfig config = ServerConfig.load();

        Label title = new Label("Chat Server");
        title.getStyleClass().add("title");

        Label usersLabel = new Label("Connected users");
        usersLabel.getStyleClass().add("section-label");
        Label logLabel = new Label("Server log");
        logLabel.getStyleClass().add("section-label");

        ListView<String> userList = new ListView<>(users);
        userList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: " + colorFor(item) + "; -fx-text-fill: #1c1c1c;");
                }
            }
        });

        logArea.setEditable(false);
        logArea.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(14);
        grid.setVgap(8);
        grid.add(title, 0, 0);
        grid.add(statusLabel, 1, 0);
        grid.add(usersLabel, 0, 1);
        grid.add(logLabel, 1, 1);
        grid.add(userList, 0, 2);
        grid.add(logArea, 1, 2);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(30);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(70);
        grid.getColumnConstraints().addAll(c0, c1);
        RowConstraints r2 = new RowConstraints();
        r2.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(new RowConstraints(), new RowConstraints(), r2);

        Scene scene = new Scene(grid, 820, 520);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("TCPServer - Group Chat");
        stage.setScene(scene);
        stage.show();

        server = new ChatServer(config, this);
        try {
            server.start();
            statusLabel.setText("Running on " + config.host() + ":" + config.port());
            statusLabel.getStyleClass().add("status-ok");
        } catch (IOException e) {
            statusLabel.setText("Failed to start");
            statusLabel.getStyleClass().add("status-bad");
            appendLog("Could not start server: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (server != null) server.stop();
    }

    // ---- ServerListener (called from background threads -> hop to FX thread) ----

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> appendLog(message));
    }

    @Override
    public void onUserJoined(String username) {
        Platform.runLater(() -> {
            if (!users.contains(username)) users.add(username);
        });
    }

    @Override
    public void onUserLeft(String username) {
        Platform.runLater(() -> {
            users.remove(username);
            colors.remove(username);
        });
    }

    private void appendLog(String message) {
        logArea.appendText("[" + LocalTime.now().format(TIME) + "] " + message + "\n");
    }

    /** Random (but stable per user) pastel background colour for the user list. */
    private String colorFor(String user) {
        return colors.computeIfAbsent(user, u -> {
            Color c = Color.hsb(random.nextDouble() * 360, 0.35, 0.97);
            return String.format("#%02x%02x%02x",
                    (int) Math.round(c.getRed() * 255),
                    (int) Math.round(c.getGreen() * 255),
                    (int) Math.round(c.getBlue() * 255));
        });
    }
}
