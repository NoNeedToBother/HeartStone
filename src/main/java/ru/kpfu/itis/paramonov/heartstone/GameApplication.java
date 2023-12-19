package ru.kpfu.itis.paramonov.heartstone;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.io.IOException;

public class GameApplication extends Application {

    private Stage primaryStage = null;

    private static GameApplication application = null;

    private GameServer server = null;

    private GameClient client = null;

    private GameRoom room = null;

    private User user = null;

    public GameRoom getRoom() {
        return room;
    }

    @Override
    public void start(Stage stage) throws IOException {
        application = this;
        primaryStage = stage;

        server = GameServer.getInstance();

        client = new GameClient(this);
        client.start();

        primaryStage.setOnCloseRequest(windowEvent -> {
            disconnect();
        });

        primaryStage.setTitle("HeartStone");

        FXMLLoader loader = new FXMLLoader(GameApplication.class.getResource("/fxml/login.fxml"));
        AnchorPane anchorPane = loader.load();

        Scene scene = new Scene(anchorPane);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        primaryStage.show();
    }

    public void disconnect() {
        String msg = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.SERVER)
                .setServerAction(ServerMessage.ServerAction.DISCONNECT)
                .build();
        client.sendMessage(msg);
        System.exit(0);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static GameApplication getApplication() {
        return application;
    }

    public GameServer getServer() {
        return server;
    }

    public GameClient getClient() {
        return client;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void loadScene(String resource) {
        String DEFAULT_PATH = "/fxml";
        FXMLLoader loader = new FXMLLoader(GameApplication.class.getResource(DEFAULT_PATH + resource));
        try {
            AnchorPane pane = loader.load();
            Scene scene = new Scene(pane);
            getPrimaryStage().setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        disconnect();
    }

    public static void main(String[] args) {
        launch();
    }
}