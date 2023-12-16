package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.ui.BattleCardInfo;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.ui.ManaBar;
import ru.kpfu.itis.paramonov.heartstone.util.Animations;

import java.util.ArrayList;
import java.util.List;

public class BattlefieldController {

    @FXML
    private HBox hBoxOpponentFieldCards;

    @FXML
    private ImageView cardPlacement;

    @FXML
    private HBox hBoxFieldCards;

    private GameButton btnEndTurn;

    @FXML
    private VBox vBoxBtnEndTurn;

    @FXML
    private HBox hBoxHandCards;

    @FXML
    private AnchorPane root;

    private List<Card> hand = new ArrayList<>();

    private List<Card> field = new ArrayList<>();

    private List<Card> opponentField = new ArrayList<>();

    private List<Card> deck = new ArrayList<>();

    private boolean active;

    @FXML
    private ImageView background;

    @FXML
    private ImageView handBg;

    @FXML
    private VBox vBoxCardInfo;

    @FXML
    private BattleCardInfo cardInfo;

    @FXML
    private ManaBar manaBar;

    @FXML
    private ManaBar opponentManaBar;

    private int mana = 0;

    private int maxMana = 0;

    private int opponentMana = 0;

    private int maxOpponentMana = 0;

    private Card selectedCard = null;

    private static BattlefieldController controller = null;

    public static BattlefieldController getController() {
        return controller;
    }

    @FXML
    private void initialize() {
        controller = this;
        setHandBackground();
        addEndTurnBtn(GameButton.GameButtonStyle.RED);
        addCardPlacements();
        makeCardInfoWrapText();
        manaBar.setMana(0, 0);
        opponentManaBar.setMana(0, 0);
    }

    private void setHeroes() {

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
                active = false;

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

    private void addCardPlacements() {
        cardPlacement.setImage(new Image(GameApplication.class.getResource("/assets/images/card_placement.png").toString()));
        cardPlacement.setOnMouseClicked(mouseEvent -> {
            if (selectedCard != null && active) {
                if (getHandCardByImageView(selectedCard.getAssociatedImageView()) == null) {
                    mouseEvent.consume();
                    return;
                }
                String msg = ServerMessage.builder()
                        .setEntityToConnect(ServerMessage.Entity.ROOM)
                        .setRoomAction(GameRoom.RoomAction.CHECK_CARD_PLAYED)
                        .setParameter("hand_pos", String.valueOf(hand.indexOf(selectedCard)))
                        .build();
                GameApplication.getApplication().getClient().sendMessage(msg);
            }
            mouseEvent.consume();
        });
    }

    public void placeCard(int handPos) {
        Card card = hand.get(handPos);

        String msg = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.ROOM)
                .setRoomAction(GameRoom.RoomAction.PLAY_CARD)
                .setParameter("pos", String.valueOf(handPos))
                .build();
        GameApplication.getApplication().getClient().sendMessage(msg);

        ImageView cardIv = card.getAssociatedImageView();
        onCardDeselected(cardIv);

        manaBar.setMana(mana - card.getCost(), maxMana);
        mana -= card.getCost();

        hBoxHandCards.getChildren().remove(cardIv);
        hand.remove(card);
        field.add(card);
        hBoxFieldCards.getChildren().add(cardIv);
        setOnHoverListener(cardIv, "field");
    }

    public void changeOpponentMana(int newOpponentMana) {
        opponentMana = newOpponentMana;
        opponentManaBar.setMana(newOpponentMana, maxOpponentMana);
    }

    public void addOpponentCard(JSONObject json) {
        Card card = getCard(json);
        Image sprite = Card.SpriteBuilder()
                .addImage(card.getCardInfo().getPortraitUrl())
                .setStyle(Card.CardStyle.BASE.toString())
                .addRarity(card.getCardInfo().getRarity())
                .scale(2)
                .build();

        ImageView cardIv = new ImageView();
        cardIv.setImage(sprite);

        opponentField.add(card);
        setOnHoverListener(cardIv, "opponent_field");

        card.associateImageView(cardIv);

        cardIv.setOnMouseClicked(mouseEvent -> {
            if (!active || selectedCard == null) {
                mouseEvent.consume();
                return;
            }
            Card handCard = getHandCardByImageView(selectedCard.getAssociatedImageView());
            if (handCard != null) {
                mouseEvent.consume();
                return;
            }
            Card selected = getOpponentFieldCardByImageView(cardIv);

            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.ROOM)
                    .setRoomAction(GameRoom.RoomAction.CARD_CARD_ATTACK)
                    .setParameter("attacked_pos", String.valueOf(opponentField.indexOf(selected)))
                    .setParameter("attacker_pos", String.valueOf(field.indexOf(selectedCard)))
                    .build();

            onCardDeselected(selectedCard.getAssociatedImageView());

            GameApplication.getApplication().getClient().sendMessage(msg);
        });

        hBoxOpponentFieldCards.getChildren().add(cardIv);
    }

    public void updateCards(JSONObject json) {
        JSONArray thisChanges = json.getJSONArray("stat_changes");
        JSONArray opponentChanges = json.getJSONArray("opponent_stat_changes");

        for (int i = 0; i < thisChanges.length(); i++) {
            JSONObject cardChange = thisChanges.getJSONObject(i);
            applyChanges(field, cardChange);
        }

        for (int i = 0; i < opponentChanges.length(); i++) {
            JSONObject cardChange = opponentChanges.getJSONObject(i);
            applyChanges(opponentField, cardChange);
        }
    }

    private void applyChanges(List<Card> field, JSONObject cardChange) {
        int pos = cardChange.getInt("pos");
        int hp = cardChange.getInt("hp");
        int atk = cardChange.getInt("atk");
        Card cardToChange = field.get(pos);
        cardToChange.setHp(hp);
        cardToChange.setAtk(atk);
        if (cardToChange.getHp() <= 0) {
            if (this.field.contains(cardToChange)) {
                onCardDeselected(cardToChange.getAssociatedImageView());
            }
            field.remove(cardToChange);
            Animations.playCardCrackingAnimation(cardToChange.getAssociatedImageView(), this);
        }
    }

    public void deleteCard(ImageView iv) {
        hBoxFieldCards.getChildren().remove(iv);
        hBoxOpponentFieldCards.getChildren().remove(iv);
    }

    private Card getCard(JSONObject json) {
        return new Card(
                json.getInt("id"),
                json.getInt("hp"),
                json.getInt("atk"),
                json.getInt("cost")
        );
    }

    private void onCardSelected(ImageView card) {
        if (selectedCard != null) {
            deselectCard(selectedCard.getAssociatedImageView());
        }
        Card selected = getHandCardByImageView(card);
        if (selected == null) selected = getFieldCardByImageView(card);
        selectedCard = selected;
        selectCard(card);
    }

    private void onCardDeselected(ImageView card) {
        selectedCard = null;
        deselectCard(card);
    }

    private void selectCard(ImageView card) {
        Image sprite = Card.SpriteBuilder()
                .addImage(selectedCard.getCardInfo().getPortraitUrl())
                .setStyle(Card.CardStyle.SELECTED.toString())
                .addRarity(selectedCard.getCardInfo().getRarity())
                .scale(2)
                .build();

        card.setImage(sprite);
    }

    private void deselectCard(ImageView card) {
        Card deselected = getHandCardByImageView(card);
        if (deselected == null) deselected = getFieldCardByImageView(card);
        Image sprite = Card.SpriteBuilder()
                .addImage(deselected.getCardInfo().getPortraitUrl())
                .setStyle(Card.CardStyle.BASE.toString())
                .addRarity(deselected.getCardInfo().getRarity())
                .scale(2)
                .build();

        card.setImage(sprite);
    }

    public void setHand(JSONArray cards) {
        ObservableList<Node> hBoxCardsChildren = hBoxHandCards.getChildren();
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

        setHandCard(atk, hp, cost, cardInfo, hBox);
    }

    public void addCardToHand(JSONObject card) {
        ObservableList<Node> hBoxCardsChildren = hBoxHandCards.getChildren();

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

    private void setHandCard(int atk, int hp, int cost, CardRepository.CardTemplate cardInfo, ObservableList<Node> layoutCards) {
        Image sprite = Card.SpriteBuilder()
                .addImage(cardInfo.getPortraitUrl())
                .setStyle(Card.CardStyle.BASE.toString())
                .addRarity(cardInfo.getRarity())
                .scale(2)
                .build();

        ImageView img = new ImageView();
        img.setImage(sprite);

        Card card = new Card(cardInfo);
        card.setAtk(atk);
        card.setHp(hp);
        card.setCost(cost);
        card.associateImageView(img);
        setOnHoverListener(img, "hand");
        hand.add(card);

        img.setOnMouseClicked(mouseEvent -> {
            if (selectedCard != null) {
                Card imgCard = getHandCardByImageView(img);
                if (imgCard == null) imgCard = getFieldCardByImageView(img);
                if (imgCard == selectedCard) onCardDeselected(img);
                else onCardSelected(img);
            }
            else onCardSelected(img);
            mouseEvent.consume();
        });
        layoutCards.add(img);
    }

    private Card getHandCardByImageView(ImageView iv) {
        for (Card card : hand) {
            if (iv.equals(card.getAssociatedImageView())) return card;
        }
        return null;
    }

    private Card getFieldCardByImageView(ImageView iv) {
        for (Card card : field) {
            if (iv.equals(card.getAssociatedImageView())) return card;
        }
        return null;
    }

    private Card getOpponentFieldCardByImageView(ImageView iv) {
        for (Card card : opponentField) {
            if (iv.equals(card.getAssociatedImageView())) return card;
        }
        return null;
    }

    private void setOnHoverListener(ImageView iv, String place) {
        iv.hoverProperty().addListener(((observableValue, oldValue, isHovered) -> {
            Card card = null;
            switch (place) {
                case "hand" -> card = getHandCardByImageView(iv);
                case "opponent_field" -> card = getOpponentFieldCardByImageView(iv);
                case "field" -> card = getFieldCardByImageView(iv);
            }
            if (card == null) return;
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

    public void setBackground(String bg) {
        String url = GameApplication.class.getResource("/assets/images/background/" + bg).toString();
        background.setImage(new Image(url));
    }

    public void setHandBackground() {
        String url = GameApplication.class.getResource("/assets/images/hand_bg.png").toString();
        handBg.setImage(new Image(url));
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setMana(JSONObject json) {
        Integer mana = null;
        Integer maxMana = null;
        Integer opponentMana = null;
        Integer maxOpponentMana = null;
        try {
            mana = json.getInt("mana");
        } catch (JSONException e) {}
        try {
            maxMana = json.getInt("maxMana");
        } catch (JSONException e) {}
        try {
            opponentMana = json.getInt("opponentMana");
        } catch (JSONException e) {}
        try {
            maxOpponentMana = json.getInt("maxOpponentMana");
        } catch (JSONException e) {}
        setMana(mana, maxMana, opponentMana, maxOpponentMana);
        updateMana();
    }

    private void setMana(Integer mana, Integer maxMana, Integer opponentMana, Integer maxOpponentMana) {
        if (mana != null) this.mana = mana;
        if (maxMana != null) this.maxMana = maxMana;
        if (opponentMana != null) this.opponentMana = opponentMana;
        if (maxOpponentMana != null) this.maxOpponentMana = maxOpponentMana;
    }

    private void updateMana() {
        manaBar.setMana(mana, maxMana);
        opponentManaBar.setMana(opponentMana, maxOpponentMana);
    }
}
