package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.util.PasswordUtil;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.ui.GameMessage;
import ru.kpfu.itis.paramonov.heartstone.util.ScaleFactor;

public class LoginController {
    @FXML
    private AnchorPane root;

    @FXML
    private ImageView logo;

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
        addLogo();
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
                .scale(ScaleFactor.MEDIUM_MENU_BTN)
                .build();
        this.btnLogin = btnLogin;

        GameButton btnGoToRegister = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BASE)
                .setText(GameButton.GameButtonText.GO_REGISTER)
                .scale(ScaleFactor.MEDIUM_MENU_BTN)
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
                    .addParameter("login", login.getText())
                    .addParameter("password", PasswordUtil.encrypt(password.getText()))
                    .build();
            client.sendMessage(msg);
            mouseEvent.consume();
        });

        btnGoToRegister.setOnMouseClicked(mouseEvent -> {
            application.loadScene("/register.fxml");
            mouseEvent.consume();
        });
    }
    private void addLogo() {
        String src = GameApplication.class.getResource("/assets/images/logo.png").toString();
        logo.setImage(new Image(src));
    }

    public void showMessage(String reason, int duration) {
        GameMessage.make(reason).show(root, duration, 625, 680);
    }

    public static LoginController getController() {
        return controller;
    }
}
