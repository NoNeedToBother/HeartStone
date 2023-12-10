package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

public class MainMenuController {

    @FXML
    private VBox mainMenu;

    private GameButton btnPlay;

    private GameButton btnDeck;

    private GameButton btnQuit;

    private GameApplication application = null;

    @FXML
    private void initialize() {
        application = GameApplication.getApplication();
        addButtons();
        setOnClickListeners();
    }

    private void addButtons() {
        GameButton btnPlay = GameButton.builder()
                .setBase()
                .setText(GameButton.GameButtonText.PLAY)
                .scale(4)
                .build();
        this.btnPlay = btnPlay;

        GameButton btnDeck = GameButton.builder()
                .setBase()
                .setText(GameButton.GameButtonText.DECK)
                .scale(4)
                .build();
        this.btnDeck = btnDeck;

        GameButton btnQuit = GameButton.builder()
                .setBase()
                .setText(GameButton.GameButtonText.QUIT)
                .scale(4)
                .build();
        this.btnQuit = btnQuit;

        mainMenu.getChildren().addAll(btnPlay, btnDeck, btnQuit);
    }

    private void setOnClickListeners() {
        btnPlay.setOnMouseClicked(mouseEvent -> onPlayClicked());
        btnQuit.setOnMouseClicked(mouseEvent -> {
            System.exit(0);
        });
    }

    private void onPlayClicked() {
        GameClient client = application.getClient();
        String msg = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.SERVER)
                .setServerAction(ServerMessage.ServerAction.CONNECT)
                .build();
        client.sendMessage(msg);
    }
}
