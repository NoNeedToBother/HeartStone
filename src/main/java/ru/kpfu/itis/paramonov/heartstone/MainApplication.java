package ru.kpfu.itis.paramonov.heartstone;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;

public class MainApplication extends Application {

    private static Stage primaryStage = null;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/battlefield.fxml"));
        AnchorPane anchorPane = loader.load();

        primaryStage.setTitle("HeartStone");

        primaryStage.setScene(new Scene(anchorPane));
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}