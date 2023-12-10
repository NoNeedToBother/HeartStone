package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.database.util.PasswordUtil;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

public class RegisterController {
    @FXML
    private VBox registerMenu;

    @FXML
    private TextField login;

    @FXML
    private PasswordField password;

    @FXML
    private PasswordField confirmPassword;

    private Button btnRegister;

    private Button btnGoToLogin;

    private GameApplication application = null;

    @FXML
    private void initialize() {
        application = GameApplication.getApplication();
        addGameButtons();
        setOnClickListeners();
    }

    private void addGameButtons() {
        GameButton btnRegister = GameButton.builder()
                .setBase()
                .setText(GameButton.GameButtonText.REGISTER)
                .scale(3)
                .build();
        registerMenu.getChildren().add(btnRegister);
        this.btnRegister = btnRegister;

        GameButton btnGoToLogin = GameButton.builder()
                .setBase()
                .setText(GameButton.GameButtonText.GO_LOGIN)
                .scale(3)
                .build();
        registerMenu.getChildren().add(btnGoToLogin);
        this.btnGoToLogin = btnGoToLogin;
    }

    private void setOnClickListeners() {
        btnGoToLogin.setOnMouseClicked(mouseEvent -> {
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
