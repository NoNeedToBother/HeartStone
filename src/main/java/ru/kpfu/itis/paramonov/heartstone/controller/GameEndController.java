package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

public class GameEndController {

    private GameButton btnMainMenu;

    private GameButton btnPlay;

    @FXML
    private VBox buttons;

    @FXML
    private void initialize() {
        setButtons();
        setOnClickListeners();
    }

    private void setButtons() {
        GameButton btnPlay = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.GREEN)
                .setText(GameButton.GameButtonText.PLAY)
                .scale(4)
                .build();
        this.btnPlay = btnPlay;

        GameButton btnMainMenu = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BASE)
                .setText(GameButton.GameButtonText.MAIN_MENU)
                .scale(4)
                .build();
        this.btnMainMenu = btnMainMenu;
        buttons.getChildren().addAll(btnPlay, btnMainMenu);
    }

    private void setOnClickListeners() {
        btnPlay.setOnMouseClicked(mouseEvent -> {
            onPlayClicked();
            mouseEvent.consume();
        });

        btnMainMenu.setOnMouseClicked(mouseEvent -> {
            GameApplication.getApplication().loadScene("/main_menu.fxml");
        });
    }

    private void onPlayClicked() {
        GameClient client = GameApplication.getApplication().getClient();
        String msg = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.SERVER)
                .setServerAction(ServerMessage.ServerAction.CONNECT)
                .build();
        client.sendMessage(msg);
    }
}
