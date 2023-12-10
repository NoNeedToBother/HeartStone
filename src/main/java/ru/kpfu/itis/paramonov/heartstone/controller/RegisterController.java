package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.database.util.PasswordUtil;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;

public class RegisterController {
    @FXML
    private TextField login;

    @FXML
    private PasswordField password;

    @FXML
    private PasswordField confirmPassword;

    @FXML
    private Button btnRegister;

    @FXML
    private Button btnGoToSignIn;

    private GameApplication application = null;

    @FXML
    private void initialize() {
        application = GameApplication.getApplication();
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        btnGoToSignIn.setOnMouseClicked(mouseEvent -> {
            application.loadScene("/login.fxml");
            mouseEvent.consume();
        });

        btnRegister.setOnMouseClicked(mouseEvent -> {
            GameClient client = application.getClient();
            if (password.getText().equals(confirmPassword.getText())) {
                String msg = ServerMessage.builder()
                        .setEntityToConnect(ServerMessage.Entity.SERVER)
                        .setServerAction(ServerMessage.ServerAction.REGISTER)
                        .setParameter("login", login.getText())
                        .setParameter("password", PasswordUtil.encrypt(password.getText()))
                        .build();
                client.sendMessage(msg);
            }
            mouseEvent.consume();
        });
    }
}
