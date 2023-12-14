package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.ui.BattleCardInfo;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

import java.util.ArrayList;
import java.util.List;

public class BattlefieldController {

    private GameButton btnEndTurn;

    @FXML
    private VBox vBoxBtnEndTurn;

    @FXML
    private HBox hBoxCards;

    @FXML
    private AnchorPane root;

    private List<Card> hand = new ArrayList<>();

    private List<Card> deck = new ArrayList<>();

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
        addEndTurnBtn(GameButton.GameButtonStyle.RED);
        makeCardInfoWrapText();
        makeCardsDraggable();
    }

    public void changeEndTurnButton(GameButton.GameButtonStyle style) {
        vBoxBtnEndTurn.getChildren().remove(btnEndTurn);
        addEndTurnBtn(style);
    }
    private void addEndTurnBtn(GameButton.GameButtonStyle style) {
        GameButton btnEndTurn = GameButton.builder()
                .setStyle(style)
                .setText(GameButton.GameButtonText.END_TURN)
                .scale(3)
                .build();
        this.btnEndTurn = btnEndTurn;

        vBoxBtnEndTurn.getChildren().add(btnEndTurn);

        this.btnEndTurn.setOnMouseClicked(mouseEvent -> {
            if (this.btnEndTurn.isClickable()) {
                String msg = ServerMessage.builder()
                        .setEntityToConnect(ServerMessage.Entity.ROOM)
                        .setRoomAction(GameRoom.RoomAction.END_TURN)
                        .build();

                GameApplication.getApplication().getClient().sendMessage(msg);
                mouseEvent.consume();
            } else {
                mouseEvent.consume();
            }
        });
    }

    public void setHand(JSONArray cards) {
        hand.clear();
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();
        for (int i = 0; i < cards.length(); i++) {
            JSONObject json = cards.getJSONObject(i);
            addCardToHand(json, hBoxCardsChildren);
        }
    }

    private void addCardToHand(JSONObject card, ObservableList<Node> hBox) {
        int atk = card.getInt("atk");
        int hp = card.getInt("hp");
        int cost = card.getInt("cost");
        CardRepository.CardTemplate cardInfo = CardRepository.getCardTemplate(card.getInt("id"));

        setCard(atk, hp, cost, cardInfo, hBox);
    }

    public void addCardToHand(JSONObject card) {
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();

        addCardToHand(card, hBoxCardsChildren);
    }

    public void setDeck(JSONArray deck) {
        List<Card> tempDeck = new ArrayList<>();
        for (int i = 0; i < deck.length(); i++) {
            JSONObject json = deck.getJSONObject(i);
            CardRepository.CardTemplate card = CardRepository.getCardTemplate(json.getInt("id"));
            tempDeck.add(new Card(card));
        }
        this.deck = tempDeck;
    }

    private void setCard(int atk, int hp, int cost, CardRepository.CardTemplate cardInfo, ObservableList<Node> layoutCards) {
        Image sprite = Card.SpriteBuilder()
                .addImage(cardInfo.getPortraitUrl())
                .setBase()
                .addRarity(cardInfo.getRarity())
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
                cardInfo.setText(card.getCardInfo().getName());
                cardInfo.addTextLine(card.getCardInfo().getActionDesc());
                cardInfo.addTextLine("ATK: ");
                cardInfo.addText(String.valueOf(card.getAtk()));
                cardInfo.addTextLine("HP: ");
                cardInfo.addText(String.valueOf(card.getHp()));
                cardInfo.addTextLine("Cost: ");
                cardInfo.addText(String.valueOf(card.getCost()));
                if (!card.getCardInfo().getFaction().equals(CardRepository.Faction.NO_FACTION)) {
                    cardInfo.addTextLine("Faction: ");
                    cardInfo.addText(String.valueOf(card.getCardInfo().getFaction()).toLowerCase());
                }
                cardInfo.addTextLine("");
                for (CardRepository.KeyWord keyWord : card.getCardInfo().getKeyWords()) {
                    cardInfo.addTextLine(keyWord.getDisplayName() + ": ");
                    cardInfo.addText(keyWord.getDescription());
                }
                cardInfo.commitChanges();
                cardInfo.setVisible(true);
            }
            else {
                cardInfo.setVisible(false);
                cardInfo.clear();
            }
        }));
    }

    private void makeCardInfoWrapText() {
        cardInfo.getText().wrappingWidthProperty().bind(vBoxCardInfo.widthProperty().add(-20));
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
        String url = GameApplication.class.getResource("/assets/images/background/" + bg).toString();
        background.setImage(new Image(url));
    }

    public void setHandBackground() {
        String url = GameApplication.class.getResource("/assets/images/hand_bg.png").toString();
        handBg.setImage(new Image(url));
    }
}
