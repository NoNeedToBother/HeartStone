package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.ui.MoneyInfo;

public class MainMenuController {

    @FXML
    private VBox mainMenu;

    private GameButton btnPlay;

    private GameButton btnDeck;

    private GameButton btnPacks;

    private GameButton btnQuit;

    @FXML
    private MoneyInfo moneyInfo;

    private GameApplication application = null;

    private static MainMenuController controller;

    @FXML
    private void initialize() {
        application = GameApplication.getApplication();
        controller = this;
        addButtons();
        setMoney();
        setOnClickListeners();
    }

    public static MainMenuController getController() {
        return controller;
    }

    private void setMoney() {
        moneyInfo.setMoney(User.getInstance().getMoney());
    }

    public void setMoney(int money) {
        moneyInfo.setMoney(money);
    }

    private void addButtons() {
        GameButton btnPlay = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.GREEN)
                .setText(GameButton.GameButtonText.PLAY)
                .scale(4)
                .build();
        this.btnPlay = btnPlay;

        GameButton btnDeck = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BASE)
                .setText(GameButton.GameButtonText.DECK)
                .scale(4)
                .build();
        this.btnDeck = btnDeck;

        GameButton btnPacks = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BASE)
                .setText(GameButton.GameButtonText.OPEN_PACKS)
                .scale(4)
                .build();
        this.btnPacks = btnPacks;

        GameButton btnQuit = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.RED)
                .setText(GameButton.GameButtonText.QUIT)
                .scale(4)
                .build();
        this.btnQuit = btnQuit;

        mainMenu.getChildren().addAll(btnPlay, btnDeck, btnPacks, btnQuit);
    }

    private void setOnClickListeners() {
        btnPlay.setOnMouseClicked(mouseEvent -> {
            onPlayClicked();
            mouseEvent.consume();
        });
        btnDeck.setOnMouseClicked(mouseEvent -> {
            GameApplication.getApplication().loadScene("/deck.fxml");
        });
        btnPacks.setOnMouseClicked(mouseEvent -> {
            GameApplication.getApplication().loadScene("/packs.fxml");
            mouseEvent.consume();
        });
        btnQuit.setOnMouseClicked(mouseEvent -> {
            GameApplication.getApplication().disconnect();
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
