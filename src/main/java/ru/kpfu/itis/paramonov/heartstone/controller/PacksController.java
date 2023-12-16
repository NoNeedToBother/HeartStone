package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.json.JSONArray;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.util.Animations;

import java.util.List;

public class PacksController {

    @FXML
    private AnchorPane root;

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
        setOnClickListeners();
        setCardCoverImageView();
    }

    public static PacksController getController() {
        return controller;
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

        btn100g.setLayoutX(300);
        btn100g.setLayoutY(550);
        btn500g.setLayoutY(550);
        btn500g.setLayoutX(820);

        root.getChildren().addAll(btn100g, btn500g);
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

    private void setCardCoverImageView() {
        Image cardCover = Card.spriteBuilder()
                .addImage("/assets/images/cards/card_cover.png")
                .setStyle(Card.CardStyle.BASE.toString())
                .scale(3)
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
        CardRepository.CardTemplate cardTemplate = CardRepository.getCardTemplate(cardId);

        Image card = Card.spriteBuilder()
                .addImage(cardTemplate.getPortraitUrl())
                .setStyle(Card.CardStyle.BASE.toString())
                .addRarity(cardTemplate.getRarity())
                .scale(3)
                .build();
        cardIv.setImage(card);
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
}