package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;

public class LoginController {
    @FXML
    private TextField login;

    @FXML
    private TextField password;

    @FXML
    private TextField confirmPassword;

    @FXML
    private Button btnSignIn;

    private GameApplication application = null;

    @FXML
    private void initialize() {
        application = GameApplication.getApplication();
        setOnSignInClicked();
    }

    private void setOnSignInClicked() {
        btnSignIn.setOnMouseClicked(mouseEvent -> {
            GameClient client = application.getClient();
            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.SERVER)
                    .setServerAction(ServerMessage.ServerAction.LOGIN)
                    .setParameter("login", login.getText())
                    .setParameter("password", password.getText())
                    .build();
            client.sendMessage(msg);
        });
    }
}
