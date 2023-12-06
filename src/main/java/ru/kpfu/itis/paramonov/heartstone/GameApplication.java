package ru.kpfu.itis.paramonov.heartstone;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.io.IOException;

public class GameApplication extends Application {

    private Stage primaryStage = null;

    private static GameApplication application = null;

    private GameServer server = null;

    private GameClient client = null;

    @Override
    public void start(Stage stage) throws IOException {
        server = GameServer.getInstance();

        client = new GameClient(this);

        primaryStage = stage;
        application = this;
        FXMLLoader loader = new FXMLLoader(GameApplication.class.getResource("/battlefield.fxml"));
        AnchorPane anchorPane = loader.load();

        primaryStage.setTitle("HeartStone");

        primaryStage.setScene(new Scene(anchorPane));
        primaryStage.show();
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

    public static void main(String[] args) {
        launch();
    }
}