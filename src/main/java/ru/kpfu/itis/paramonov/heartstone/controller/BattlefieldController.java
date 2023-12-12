package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.ui.BattleCardInfo;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.util.BufferedImageUtil;

import java.util.ArrayList;
import java.util.List;

public class BattlefieldController {

    private Button btnEndTurn;

    @FXML
    private VBox vBoxBtnEndTurn;

    @FXML
    private HBox hBoxCards;

    @FXML
    private AnchorPane root;

    private List<Card> hand = new ArrayList<>();

    @FXML
    private ImageView background;

    @FXML
    private ImageView handBg;

    @FXML
    private VBox vBoxCardInfo;

    @FXML
    private BattleCardInfo cardInfo;

    private static BattlefieldController controller = null;

    public static BattlefieldController getController() {
        return controller;
    }

    @FXML
    private void initialize() {
        controller = this;
        setHandBackground();
        makeCardInfoWrapText();
        addEndTurnBtn();
        makeCardsDraggable();
    }

    private void addEndTurnBtn() {
        GameButton btnEndTurn = GameButton.builder()
                .setBase()
                .setText(GameButton.GameButtonText.END_TURN)
                .scale(3)
                .build();
        this.btnEndTurn = btnEndTurn;

        vBoxBtnEndTurn.getChildren().add(btnEndTurn);
    }

    public void setCards(JSONArray cards) {
        hand.clear();
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();
        for (int i = 0; i < cards.length(); i++) {
            JSONObject json = cards.getJSONObject(i);
            int atk = json.getInt("atk");
            int hp = json.getInt("hp");
            int cost = json.getInt("cost");
            CardRepository.CardTemplate cardInfo = CardRepository.getCardTemplate(json.getInt("id"));

            setCard(atk, hp, cost, cardInfo, hBoxCardsChildren);
        }
    }

    private void setCard(int atk, int hp, int cost, CardRepository.CardTemplate cardInfo, ObservableList<Node> layoutCards) {
        Image sprite = Card.SpriteBuilder()
                .addImage(cardInfo.getPortraitUrl())
                .addRarity(cardInfo.getRarity())
                .setBase()
                .scale(2)
                .build();

        ImageView img = new ImageView();
        img.setImage(sprite);

        layoutCards.add(img);

        Card card = new Card(cardInfo);
        card.setAtk(atk);
        card.setHp(hp);
        card.setCost(cost);
        card.associateImageView(img);
        setOnHoverListener(img);
        hand.add(card);
    }

    private Card getCardByImageView(ImageView iv) {
        for (Card card : hand) {
            if (iv.equals(card.getAssociatedImageView())) return card;
        }
        return null;
    }

    private void setOnHoverListener(ImageView iv) {
        iv.hoverProperty().addListener(((observableValue, oldValue, isHovered) -> {
            Card card = getCardByImageView(iv);
            if (isHovered) {
                cardInfo.setVisible(true);
            }
            else cardInfo.setVisible(false);
        }));
    }

    private void makeCardInfoWrapText() {
        cardInfo.getText().wrappingWidthProperty().bind(vBoxCardInfo.widthProperty());
    }

    private EventHandler<MouseEvent> getDragEventHandler(ImageView iv, Card card) {
        return mouseEvent -> {
            Dragboard db = iv.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putImage(iv.getImage());
            db.setContent(content);
            mouseEvent.consume();
        };
    }

    private void makeCardsDraggable() {
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();
        int counter = 0;

        for (Node element : hBoxCardsChildren) {
            if (element instanceof ImageView) {
                ImageView iv = (ImageView) element;
                iv.setOnDragDetected(getDragEventHandler(iv, hand.get(counter)));
                counter++;
            }
        }
    }

    public void setBackground(String bg) {
        BufferedImageUtil.getFromSrcAndSetImage("\\background\\" + bg, background);
    }

    public void setHandBackground() {
        BufferedImageUtil.getFromSrcAndSetImage("\\hand_bg.png", handBg);
    }
}
