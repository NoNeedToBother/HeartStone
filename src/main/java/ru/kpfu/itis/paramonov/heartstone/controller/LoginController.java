package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.util.PasswordUtil;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.ui.GameMessage;

public class LoginController {
    @FXML
    private AnchorPane root;

    @FXML
    private TextField login;

    @FXML
    private PasswordField password;

    private GameButton btnLogin;

    private GameButton btnGoToRegister;

    @FXML
    private VBox loginMenu;

    private GameApplication application = null;

    private static LoginController controller;

    @FXML
    private void initialize() {
        controller = this;
        application = GameApplication.getApplication();
        setTextFieldsStyle();
        addGameButtons();
        setOnClickListeners();
    }

     private void setTextFieldsStyle() {
        loginMenu.getStylesheets().add(GameApplication.class.getResource("/css/text-fields.css").toString());
     }

    private void addGameButtons() {
        GameButton btnLogin = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BASE)
                .setText(GameButton.GameButtonText.LOGIN)
                .scale(3)
                .build();
        this.btnLogin = btnLogin;

        GameButton btnGoToRegister = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BASE)
                .setText(GameButton.GameButtonText.GO_REGISTER)
                .scale(3)
                .build();
        this.btnGoToRegister = btnGoToRegister;

        loginMenu.getChildren().addAll(btnLogin, btnGoToRegister);
    }

    private void setOnClickListeners() {
        btnLogin.setOnMouseClicked(mouseEvent -> {
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

    public void showMessage(String reason, int duration) {
        GameMessage.make(reason).show(root, duration, 500, 550);
    }

    public static LoginController getController() {
        return controller;
    }
}
