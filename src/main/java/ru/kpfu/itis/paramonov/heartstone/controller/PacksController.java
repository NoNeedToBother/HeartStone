package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.ui.BattleCard;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.ui.GameMessage;
import ru.kpfu.itis.paramonov.heartstone.ui.MoneyInfo;
import ru.kpfu.itis.paramonov.heartstone.util.Animations;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;
import ru.kpfu.itis.paramonov.heartstone.util.CardImages;
import ru.kpfu.itis.paramonov.heartstone.util.ScaleFactor;

import java.util.List;

public class PacksController {

    @FXML
    private AnchorPane root;

    @FXML
    private VBox vBoxBtnBack;

    private GameButton btnBack;

    @FXML
    private ImageView cardCoverIv;

    @FXML
    private ImageView card1;
    @FXML
    private ImageView card2;
    @FXML
    private ImageView card3;
    @FXML
    private ImageView card4;
    @FXML
    private ImageView card5;

    @FXML
    private MoneyInfo moneyInfo;

    private List<ImageView> cardImageViews;

    private GameButton btn100g;

    private GameButton btn500g;

    private static PacksController controller;

    private boolean opening = false;

    @FXML
    private void initialize() {
        controller = this;
        cardImageViews = List.of(card1, card2, card3, card4, card5);
        addButtons();
        setMoney();
        setOnClickListeners();
        setCardCoverImageView();
    }

    private void setMoney() {
        moneyInfo.setMoney(User.getInstance().getMoney());
    }

    public static PacksController getController() {
        return controller;
    }

    public void showMessage(String reason, int duration) {
        GameMessage.make(reason).show(root, duration, 625, 375);
    }

    private void addButtons() {
        GameButton btn100g = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.GOLD_100)
                .scale(ScaleFactor.BIG_MENU_BTN)
                .build();
        this.btn100g = btn100g;

        GameButton btn500g = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.GOLD_500)
                .scale(ScaleFactor.BIG_MENU_BTN)
                .build();
        this.btn500g = btn500g;

        double SCALE_FACTOR = 1.25;

        btn100g.setLayoutX(300 * SCALE_FACTOR);
        btn100g.setLayoutY(550 * SCALE_FACTOR);
        btn500g.setLayoutY(550 * SCALE_FACTOR);
        btn500g.setLayoutX(820 * SCALE_FACTOR);
        root.getChildren().addAll(btn100g, btn500g);

        GameButton btnBack = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BACK)
                .scale(ScaleFactor.MEDIUM_MENU_BTN)
                .build();
        this.btnBack = btnBack;
        vBoxBtnBack.getChildren().add(btnBack);
    }

    private EventHandler<MouseEvent> getMouseEventHandler(ServerMessage.ServerAction action) {
        return mouseEvent -> {
            if (opening) {
                mouseEvent.consume();
                return;
            }
            opening = true;
            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.SERVER)
                    .setServerAction(action)
                    .addParameter("login", User.getInstance().getLogin())
                    .build();

            GameApplication.getApplication().getClient().sendMessage(msg);
            mouseEvent.consume();
        };
    }

    private void setOnClickListeners() {
        btn100g.setOnMouseClicked(getMouseEventHandler(ServerMessage.ServerAction.OPEN_1_PACK));

        btn500g.setOnMouseClicked(getMouseEventHandler(ServerMessage.ServerAction.OPEN_5_PACKS));

        btnBack.setOnMouseClicked(mouseEvent ->
                GameApplication.getApplication().loadScene("/main_menu.fxml")
        );
    }

    private void setCardCoverImageView() {
        Image cardCover = BattleCard.spriteBuilder()
                .addImage("/assets/images/cards/card_cover.png")
                .setStyle(BattleCard.CardStyle.BASE.toString())
                .scale(ScaleFactor.DEFAULT_CARD)
                .build();

        cardCoverIv.setImage(cardCover);
    }

    public void playOpeningAnimation(Integer cardId, JSONArray cardIds) {
        Animations.playPackShakingAnimation(cardCoverIv, cardId, cardIds);
    }

    public void notifyAnimationEnded() {
        opening = false;
    }

    public void showCard(int id) {
        showCard(card1, id);
    }

    private void showCard(ImageView cardIv, int cardId) {
        Image card = CardImages.getPortrait(cardId);
        cardIv.setImage(ImageUtil.scale(card, ScaleFactor.DEFAULT_CARD));
    }

    public void showCards(JSONArray jsonIds) {
        for (int i = 0; i < jsonIds.length(); i++) {
            showCard(cardImageViews.get(i), jsonIds.getInt(i));
        }
    }

    public void clearCardImageViews() {
        for (ImageView iv : cardImageViews) {
            iv.setImage(null);
        }
    }

    public void updateUserInfo(JSONObject json) {
        moneyInfo.setMoney(json.getInt("money"));
        List<CardRepository.CardTemplate> cards = CardRepository.getCardsById(json.getString("cards"));
        User.getInstance().setCards(cards);
    }
}
