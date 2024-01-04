package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.ui.*;
import ru.kpfu.itis.paramonov.heartstone.util.Animations;
import ru.kpfu.itis.paramonov.heartstone.util.CardImages;
import ru.kpfu.itis.paramonov.heartstone.util.ScaleFactor;

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

    private final List<Card> hand = new ArrayList<>();
    private final List<Card> field = new ArrayList<>();
    private final List<Card> opponentField = new ArrayList<>();

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

    private Hero player;
    private Hero opponent;

    @FXML
    private HeroInfo playerHeroInfo;
    @FXML
    private HeroInfo opponentHeroInfo;

    @FXML
    private ImageView deckCoverIv;
    @FXML
    private ImageView deckInfoIv;
    @FXML
    private Text deckInfo;

    @FXML
    private AnchorPane root;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ImageView fieldEffects;

    private Card selectedCard = null;

    private static BattlefieldController controller = null;

    private boolean attacking = false;

    public static BattlefieldController getController() {
        return controller;
    }

    public static void resetController() {
        controller = null;
    }

    @FXML
    private void initialize() {
        controller = this;
        setHandBackground();
        addEndTurnBtn(GameButton.GameButtonStyle.RED);
        addCardPlacements();
        setDeckInfo();
        makeCardInfoWrapText();
        manaBar.setMana(0, 0);
        opponentManaBar.setMana(0, 0);
        fieldEffects.setImage(new Image(
                GameApplication.class.getResource("/assets/animations/empty_field_effects.png").toString()));
    }

    private void setDeckInfo() {
        Image deckCover = Card.spriteBuilder()
                .addImage("/assets/images/cards/card_cover.png")
                .setStyle(Card.CardStyle.BASE.toString())
                .build();
        deckCoverIv.setImage(deckCover);

        deckInfoIv.setImage(new Image(GameApplication.class.getResource("/assets/images/mana_bar/text_bg.png").toString()));
    }

    public void setHeroes(JSONObject json) {
        int playerHp = json.getInt("hp");
        int opponentHp = json.getInt("opponent_hp");
        player = new Hero(playerHp, playerHp, 0, 0);
        opponent = new Hero(opponentHp, opponentHp, 0, 0);

        Image portrait = Hero.spriteBuilder()
                .addImage("/standard_hero.png")
                .setStyle(Hero.HeroStyle.BASE.toString())
                .build();
        playerHeroInfo.setPortrait(portrait);
        opponentHeroInfo.setPortrait(portrait);
        playerHeroInfo.changeHealth(playerHp);
        opponentHeroInfo.changeHealth(opponentHp);

        ImageView opponentPortrait = opponentHeroInfo.getPortrait();
        opponentPortrait.setOnMouseClicked(mouseEvent -> {
            if (selectedCard == null || hand.contains(selectedCard)) {
                mouseEvent.consume();
                return;
            }
            if (checkAttacking(mouseEvent)) return;

            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.ROOM)
                    .setRoomAction(GameRoom.RoomAction.CHECK_CARD_TO_ATTACK)
                    .addPosition("pos", field.indexOf(selectedCard))
                    .addParameter("target", "hero")
                    .build();

            GameApplication.getApplication().getClient().sendMessage(msg);
        });
    }
    public void onGameEnd(JSONObject json) {
        User.getInstance().setMoney(json.getInt("money"));
        switch (json.getString("result")) {
            case "win" -> Animations.playHeroCrackingAnimation(opponentHeroInfo.getPortrait(), true);
            case "defeat" -> Animations.playHeroCrackingAnimation(playerHeroInfo.getPortrait(), false);
        }
    }

    public void showMessage(String reason) {
        GameMessage.make(reason).show(root, 800, 625, 375);
    }

    public void attack(Integer pos, Integer opponentPos, String target) {
        String msg = null;
        switch (target) {
            case "hero" ->
                msg = ServerMessage.builder()
                        .setEntityToConnect(ServerMessage.Entity.ROOM)
                        .setRoomAction(GameRoom.RoomAction.CARD_HERO_ATTACK)
                        .addPosition("pos", pos)
                        .build();
            case "card" -> {
                msg = ServerMessage.builder()
                        .setEntityToConnect(ServerMessage.Entity.ROOM)
                        .setRoomAction(GameRoom.RoomAction.CARD_CARD_ATTACK)
                        .addPosition("attacked_pos", opponentPos)
                        .addPosition("attacker_pos", pos)
                        .build();

                onCardDeselected(selectedCard.getAssociatedImageView());
            }
        }
        GameApplication.getApplication().getClient().sendMessage(msg);
    }

    public void playFieldFireAnimation() {
        Animations.playFieldFireAnimation(fieldEffects);
    }

    public void playAttackingAnimation(JSONObject json) {
        attacking = true;
        ImageView card;
        try {
            int pos = json.getInt("pos");
            int opponentPos = json.getInt("opponent_pos");
            if (json.getString("role").equals("attacker"))
                Animations.playCardAttacking(field.get(pos).getAssociatedImageView(), opponentField.get(opponentPos).getAssociatedImageView());
            else Animations.playCardAttacking(opponentField.get(opponentPos).getAssociatedImageView(), field.get(pos).getAssociatedImageView());
            return;
        } catch (JSONException ignored) {}
        try {
            int pos = json.getInt("field_pos");
            card = field.get(pos).getAssociatedImageView();
            Animations.playCardAttacking(card, opponentHeroInfo.getPortrait());
        } catch (JSONException e) {
            int pos = json.getInt("opponent_field_pos");
            card = opponentField.get(pos).getAssociatedImageView();
            Animations.playCardAttacking(card, playerHeroInfo.getPortrait());
        }
    }
    public void notifyAttackingAnimationStopped() {
        attacking = false;
    }
    public void updateHp(JSONObject json) {
        Integer hp = getIntParam(json, "hp");
        playerHeroInfo.changeHealth(hp);
        Integer opponentHp = getIntParam(json, "opponent_hp");
        opponentHeroInfo.changeHealth(opponentHp);
    }

    public void applyChange(JSONObject json) {
        Integer pos = getIntParam(json, "pos");
        if (pos != null) applyChanges(json, pos, field);

        pos = getIntParam(json, "opponent_pos");
        if (pos != null) applyChanges(json, pos, opponentField);

        try {
            int stolenPos = json.getInt("stolen_pos");
            if (field.get(stolenPos).equals(selectedCard)) {
                onCardDeselected(selectedCard.getAssociatedImageView());
            }
            addOpponentCard(json);
            Card stolen = field.remove(stolenPos);
            onCardDeselected(stolen.getAssociatedImageView());
            hBoxFieldCards.getChildren().remove(stolen.getAssociatedImageView());
        } catch (JSONException ignored) {}

        try {
            int gottenPos = json.getInt("gotten_pos");
            Card card = getCard(json);
            Image sprite = CardImages.getPortrait(card.getCardInfo().getId());
            ImageView iv = new ImageView(sprite);
            card.associateImageView(iv);
            Card gotten = opponentField.remove(gottenPos);
            hBoxOpponentFieldCards.getChildren().remove(gotten.getAssociatedImageView());
            field.add(card);
            hBoxFieldCards.getChildren().add(iv);
            setSelectionBehaviour(iv);
            setOnHoverListener(iv, "field");
        } catch (JSONException ignored) {}

        Integer handPos = getIntParam(json, "hand_pos");
        if (handPos != null) {
            Card card = hand.get(handPos);
            card.setCost(json.getInt("cost"));
        }
    }

    private void setSelectionBehaviour(ImageView iv) {
        iv.setOnMouseClicked(mouseEvent -> {
            if (selectedCard != null) {
                Card imgCard = getHandCardByImageView(iv);
                if (imgCard == null) imgCard = getFieldCardByImageView(iv);
                if (imgCard == selectedCard) onCardDeselected(iv);
                else onCardSelected(iv);
            } else onCardSelected(iv);
            mouseEvent.consume();
        });
    }

    private void checkShieldStatus(JSONObject json, Card card) {
        String shieldStatus = getStringParam(json, "shield_status");
        if (shieldStatus != null) {
            if (shieldStatus.equals("removed")) {
                card.removeStatus(CardRepository.Status.SHIELDED);
                CardImages.removeShield(card);
            }
        }
    }

    private void applyChanges(JSONObject json, int pos, List<Card> field) {
        Card targeted = field.get(pos);
        Integer hp = getIntParam(json, "hp");
        Integer atk = getIntParam(json, "atk");
        String status = getStringParam(json, "card_status");
        checkShieldStatus(json, field.get(pos));
        applyChange(field, targeted, pos, hp, atk, status);
    }

    private void applyChange(List<Card> field, Card damaged, int pos, Integer hp, Integer atk, String status) {
        if (hp != null && hp <= 0) {
            field.remove(damaged);
            Animations.playCardCrackingAnimation(damaged.getAssociatedImageView(), this);
        }
        else {
            Card card = field.get(pos);
            if (hp != null) card.setHp(hp);
            if (atk != null) card.setAtk(atk);
            if (status != null) {
                switch (status) {
                    case "no_frozen" -> {
                        Animations.playUnfreezingAnimation(field.get(pos));
                        field.get(pos).removeStatus(CardRepository.Status.FROZEN);
                    }
                    case "FROZEN" -> {
                        if (!field.get(pos).hasStatus(CardRepository.Status.FROZEN)) {
                            field.get(pos).addStatus(CardRepository.Status.valueOf(status));
                            Animations.playFreezingAnimation(field.get(pos).getAssociatedImageView());
                        }
                    }
                    case "no_aligned" -> card.removeStatus(card.getCurrentAlignedStatus());
                    default -> card.addJustStatus(CardRepository.Status.valueOf(status));
                }
            }
        }
    }

    public void changeEndTurnButton(GameButton.GameButtonStyle style) {
        vBoxBtnEndTurn.getChildren().remove(btnEndTurn);
        addEndTurnBtn(style);
    }
    private void addEndTurnBtn(GameButton.GameButtonStyle style) {
        GameButton btnEndTurn = GameButton.builder()
                .setStyle(style)
                .setText(GameButton.GameButtonText.END_TURN)
                .scale(ScaleFactor.MEDIUM_MENU_BTN)
                .build();
        this.btnEndTurn = btnEndTurn;

        vBoxBtnEndTurn.getChildren().add(btnEndTurn);

        this.btnEndTurn.setOnMouseClicked(mouseEvent -> {
            if (this.btnEndTurn.isClickable()) {
                progressBar.setProgress(1.0);

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

    private boolean checkAttacking(MouseEvent mouseEvent) {
        if (attacking) {
            mouseEvent.consume();
            return true;
        }
        return false;
    }

    private void addCardPlacements() {
        cardPlacement.setImage(new Image(GameApplication.class.getResource("/assets/images/card_placement.png").toString()));
        cardPlacement.setOnMouseClicked(mouseEvent -> {
            if (selectedCard != null) {
                if (getHandCardByImageView(selectedCard.getAssociatedImageView()) == null) {
                    mouseEvent.consume();
                    return;
                }
                if (checkAttacking(mouseEvent)) return;
                ServerMessage.ServerMessageBuilder msg = ServerMessage.builder()
                        .setEntityToConnect(ServerMessage.Entity.ROOM)
                        .setRoomAction(GameRoom.RoomAction.CHECK_CARD_PLAYED)
                        .addPosition("hand_pos", hand.indexOf(selectedCard));
                if (selectedCard.getCardInfo().getKeyWords().contains(CardRepository.KeyWord.BATTLE_CRY)) msg.addParameter("card_action", "action");
                GameApplication.getApplication().getClient().sendMessage(msg.build());
            }
            mouseEvent.consume();
        });
    }

    public void playCard(JSONObject json) {
        int handPos = json.getInt("hand_pos");

        ServerMessage.ServerMessageBuilder builder = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.ROOM)
                .setRoomAction(GameRoom.RoomAction.PLAY_CARD)
                .addPosition("pos", handPos);

        try {
            String cardAction = json.getString("card_action");
            builder.addParameter("card_action", cardAction);
            switch (cardAction) {
                case "deal_dmg" -> builder.addParameter("opponent_pos",
                        String.valueOf(json.getInt("opponent_pos")));
            }
        } catch (JSONException ignored) {}

        GameApplication.getApplication().getClient().sendMessage(builder.build());
    }

    public void placeCard(JSONObject json) {
        int handPos = json.getInt("pos");
        Card card = hand.get(handPos);
        addStatuses(card, json);
        ImageView cardIv = card.getAssociatedImageView();
        onCardDeselected(cardIv);

        int newMana = json.getInt("mana");
        manaBar.setMana(newMana, player.getMaxMana());
        player.setMana(newMana);

        hBoxHandCards.getChildren().remove(cardIv);
        hand.remove(card);
        field.add(card);
        if (card.getStatuses().contains(CardRepository.Status.SHIELDED)) CardImages.addShield(cardIv);
        if (card.getCardInfo().getKeyWords().contains(CardRepository.KeyWord.TAUNT)) CardImages.addTaunt(cardIv);
        hBoxFieldCards.getChildren().add(cardIv);
        setOnHoverListener(cardIv, "field");
    }

    public void changeOpponentMana(int newOpponentMana) {
        opponent.setMana(newOpponentMana);
        opponentManaBar.setMana(newOpponentMana, opponent.getMaxMana());
    }

    public void addOpponentCard(JSONObject json) {
        Card card = getCard(json);
        Image sprite = CardImages.getPortrait(card.getCardInfo().getId());

        ImageView cardIv = new ImageView();
        cardIv.setImage(sprite);

        opponentField.add(card);
        setOnHoverListener(cardIv, "opponent_field");

        card.associateImageView(cardIv);

        cardIv.setOnMouseClicked(mouseEvent -> {
            if (selectedCard == null) {
                mouseEvent.consume();
                return;
            }
            if (checkAttacking(mouseEvent)) return;

            Card handCard = getHandCardByImageView(selectedCard.getAssociatedImageView());
            Card selected = getOpponentFieldCardByImageView(cardIv);

            if (handCard != null) {
                if ((handCard.getCardInfo().getActions().contains(CardRepository.Action.DAMAGE_ENEMY_ON_PLAY) ||
                        handCard.getCardInfo().getActions().contains(CardRepository.Action.DESTROY_ENEMY_ON_PLAY)) ||
                        handCard.getCardInfo().getActions().contains(CardRepository.Action.FREEZE_ENEMY_ON_PLAY)) {
                    sendAttackingOnPlay(handCard, selected);
                    mouseEvent.consume();
                    return;
                }
            }

            if (handCard != null) {
                mouseEvent.consume();
                return;
            }

            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.ROOM)
                    .setRoomAction(GameRoom.RoomAction.CHECK_CARD_TO_ATTACK)
                    .addPosition("pos", field.indexOf(selectedCard))
                    .addPosition("opponent_pos", opponentField.indexOf(selected))
                    .addParameter("target", "card")
                    .build();

            GameApplication.getApplication().getClient().sendMessage(msg);
        });
        if (card.getStatuses().contains(CardRepository.Status.SHIELDED)) CardImages.addShield(cardIv);
        if (card.getCardInfo().getKeyWords().contains(CardRepository.KeyWord.TAUNT)) CardImages.addTaunt(cardIv);
        hBoxOpponentFieldCards.getChildren().add(cardIv);
    }

    private void sendAttackingOnPlay(Card handCard, Card opponentCard) {
        String msg = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.ROOM)
                .setRoomAction(GameRoom.RoomAction.CHECK_CARD_PLAYED)
                .addPosition("hand_pos", hand.indexOf(handCard))
                .addPosition("opponent_pos", opponentField.indexOf(opponentCard))
                .addParameter("card_action", "deal_dmg")
                .build();
        GameApplication.getApplication().getClient().sendMessage(msg);
    }

    public void updateCards(JSONObject json) {
        JSONArray changes = json.getJSONArray("stat_changes");
        JSONArray opponentChanges = json.getJSONArray("opponent_stat_changes");

        updateCards(changes, field);
        updateCards(opponentChanges, opponentField);
    }

    private void updateCards(JSONArray changes, List<Card> field) {
        List<Card> defeatedCards = new ArrayList<>();
        for (int i = 0; i < changes.length(); i++) {
            JSONObject cardChange = changes.getJSONObject(i);
            Card card = applyChanges(field, cardChange);
            if (card.getHp() <= 0) defeatedCards.add(card);
        }
        removeCardsFrom(defeatedCards, field);
    }

    private void removeCardsFrom(List<Card> cards, List<Card> from) {
        for (Card card : cards) {
            Animations.playCardCrackingAnimation(card.getAssociatedImageView(), this);
        }
        from.removeAll(cards);
    }

    private Card applyChanges(List<Card> field, JSONObject cardChange) {
        int pos = cardChange.getInt("pos");
        Card cardToChange = field.get(pos);
        Integer hp = getIntParam(cardChange, "hp");
        if (hp != null) cardToChange.setHp(hp);
        Integer atk = getIntParam(cardChange, "atk");
        if (atk != null) cardToChange.setAtk(atk);
        String status = getStringParam(cardChange, "card_status");
        if (status != null) {
            if (status.equals(CardRepository.Status.FROZEN.toString())) {
                cardToChange.addStatus(CardRepository.Status.FROZEN);
                Animations.playFreezingAnimation(cardToChange.getAssociatedImageView());
            }
        }
        String alignedStatus = getStringParam(cardChange, "aligned_status");
        if (alignedStatus != null) {
            if (!alignedStatus.equals("no_aligned")) cardToChange.addJustStatus(CardRepository.Status.valueOf(alignedStatus));
            else cardToChange.removeStatus(cardToChange.getCurrentAlignedStatus());
        }
        checkShieldStatus(cardChange, cardToChange);
        return cardToChange;
    }

    public void deleteCard(ImageView iv) {
        hBoxFieldCards.getChildren().remove(iv);
        hBoxOpponentFieldCards.getChildren().remove(iv);
    }

    private Card getCard(JSONObject json) {
        Card card = new Card(
                json.getInt("id"),
                json.getInt("hp"),
                json.getInt("atk"),
                json.getInt("cost")
        );
        addStatuses(card, json);
        return card;
    }

    private void addStatuses(Card card, JSONObject json) {
        try {
            JSONArray statuses = json.getJSONArray("statuses");
            for (int i = 0; i < statuses.length(); i++) {
                CardRepository.Status status = CardRepository.Status.valueOf(statuses.getString(i));
                if (!status.isUtility()) card.addStatus(status);
            }
        } catch (JSONException ignored) {}
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
        Image sprite = Card.spriteBuilder()
                .addImage(selectedCard.getCardInfo().getPortraitUrl())
                .setStyle(Card.CardStyle.SELECTED.toString())
                .addRarity(selectedCard.getCardInfo().getRarity())
                .build();

        card.setImage(sprite);
    }

    private void deselectCard(ImageView card) {
        Card deselected = getHandCardByImageView(card);
        if (deselected == null) deselected = getFieldCardByImageView(card);
        if(deselected == null) return;
        Image sprite = CardImages.getPortrait(deselected.getCardInfo().getId());

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

    public void setDeckSize(int deckSize) {
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/ThaleahFat.ttf").toString(), 16);
        deckInfo.setFont(font);
        deckInfo.setText(deckSize + " cards");
        if (deckSize == 0) deckCoverIv.setImage(null);
    }

    private void setHandCard(int atk, int hp, int cost, CardRepository.CardTemplate cardInfo, ObservableList<Node> layoutCards) {
        Image sprite = CardImages.getPortrait(cardInfo.getId());

        ImageView img = new ImageView();
        img.setImage(sprite);

        Card card = new Card(cardInfo);
        card.setAtk(atk);
        card.setHp(hp);
        card.setCost(cost);
        card.associateImageView(img);
        setOnHoverListener(img, "hand");
        hand.add(card);

        setSelectionBehaviour(img);
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
                cardInfo.updateInfo(card);
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

    public void setMana(JSONObject json) {
        Integer mana = getIntParam(json, "mana");
        Integer maxMana = getIntParam(json, "maxMana");
        Integer opponentMana = getIntParam(json, "opponentMana");
        Integer maxOpponentMana = getIntParam(json, "maxOpponentMana");
        setMana(mana, maxMana, opponentMana, maxOpponentMana);
        updateMana();
    }

    private Integer getIntParam(JSONObject json, String param) {
        Integer res = null;
        try {
            res = json.getInt(param);
        } catch (JSONException ignored) {}
        return res;
    }

    private String getStringParam(JSONObject json, String param) {
        String res = null;
        try {
            res = json.getString(param);
        } catch (JSONException ignored) {}
        return res;
    }

    private void setMana(Integer mana, Integer maxMana, Integer opponentMana, Integer maxOpponentMana) {
        if (mana != null) player.setMana(mana);
        if (maxMana != null) player.setMaxMana(maxMana);
        if (opponentMana != null) opponent.setMana(opponentMana);
        if (maxOpponentMana != null) opponent.setMaxMana(maxOpponentMana);
    }

    private void updateMana() {
        manaBar.setMana(player.getMana(), player.getMaxMana());
        opponentManaBar.setMana(opponent.getMana(), opponent.getMaxMana());
    }

    public void handleTimer(JSONObject json) {
        if (json.getString("status").equals("end")) {
            progressBar.setProgress(1.0);
            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.ROOM)
                    .setRoomAction(GameRoom.RoomAction.END_TURN)
                    .build();

            GameApplication.getApplication().getClient().sendMessage(msg);
        } else {
            int seconds = Integer.parseInt(json.getString("status"));
            progressBar.setProgress(1 - (double) seconds / json.getInt("maxSeconds"));
        }
    }
}
