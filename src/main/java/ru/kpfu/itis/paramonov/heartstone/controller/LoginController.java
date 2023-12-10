package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.database.util.PasswordUtil;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;

public class LoginController {
    @FXML
    private TextField login;

    @FXML
    private PasswordField password;

    @FXML
    private Button btnSignIn;

    @FXML
    private Button btnGoToRegister;

    private GameApplication application = null;

    @FXML
    private void initialize() {
        application = GameApplication.getApplication();
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        btnSignIn.setOnMouseClicked(mouseEvent -> {
            GameClient client = application.getClient();
            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.SERVER)
                    .setServerAction(ServerMessage.ServerAction.LOGIN)
                    .setParameter("login", login.getText())
                    .setParameter("password", PasswordUtil.encrypt(password.getText()))
                    .build();
            client.sendMessage(msg);
            mouseEvent.consume();
        });

        btnGoToRegister.setOnMouseClicked(mouseEvent -> {
            application.loadScene("/register.fxml");
            mouseEvent.consume();
        });
    }
}
