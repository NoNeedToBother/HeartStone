package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

public class PacksController {

    @FXML
    private AnchorPane root;

    private GameButton btn100g;

    private GameButton btn500g;

    @FXML
    private void initialize() {
        addButtons();
        setOnClickListeners();
    }

    private void addButtons() {
        GameButton btn100g = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.GOLD_100)
                .scale(4)
                .build();
        this.btn100g = btn100g;

        GameButton btn500g = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.GOLD_500)
                .scale(4)
                .build();
        this.btn500g = btn500g;

        btn100g.setLayoutX(200);
        btn100g.setLayoutY(550);
        btn500g.setLayoutY(550);
        btn500g.setLayoutX(920);

        root.getChildren().addAll(btn100g, btn500g);
    }

    private EventHandler<MouseEvent> getMouseEventHandler(ServerMessage.ServerAction action) {
        return mouseEvent -> {
            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.SERVER)
                    .setServerAction(action)
                    .setParameter("login", User.getInstance().getLogin())
                    .build();

            GameApplication.getApplication().getClient().sendMessage(msg);
            mouseEvent.consume();
        };
    }

    private void setOnClickListeners() {
        btn100g.setOnMouseClicked(getMouseEventHandler(ServerMessage.ServerAction.OPEN_1_PACK));

        btn500g.setOnMouseClicked(getMouseEventHandler(ServerMessage.ServerAction.OPEN_5_PACKS));
    }
}
