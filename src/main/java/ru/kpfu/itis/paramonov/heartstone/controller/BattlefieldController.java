package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.ui.*;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.Animation;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.animation.*;
import ru.kpfu.itis.paramonov.heartstone.ui.battle.BattleCard;
import ru.kpfu.itis.paramonov.heartstone.ui.battle.HeroInfo;
import ru.kpfu.itis.paramonov.heartstone.ui.battle.ManaBar;
import ru.kpfu.itis.paramonov.heartstone.util.CardImages;
import ru.kpfu.itis.paramonov.heartstone.util.ScaleFactor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private AnchorPane opponentHand;

    private final List<BattleCard> hand = new ArrayList<>();
    private final List<BattleCard> field = new ArrayList<>();
    private final List<BattleCard> opponentField = new ArrayList<>();

    @FXML
    private ImageView background;

    @FXML
    private ImageView handBg;

    @FXML
    private VBox vBoxCardInfo;

    @FXML
    private CardInfo cardInfo;

    @FXML
    private ManaBar manaBar;
    @FXML
    private ManaBar opponentManaBar;

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

    private BattleCard selectedCard = null;

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
        Image deckCover = BattleCard.spriteBuilder()
                .addImage("/assets/images/cards/card_cover.png")
                .setStyle(BattleCard.CardStyle.BASE.toString())
                .build();
        deckCoverIv.setImage(deckCover);

        deckInfoIv.setImage(new Image(GameApplication.class.getResource("/assets/images/mana_bar/text_bg.png").toString()));
    }

    public void setHeroes(JSONObject json) {
        int playerHp = json.getInt("hp");
        int opponentHp = json.getInt("opponent_hp");
        Hero player = new Hero(playerHp, playerHp, 0, 0);
        Hero opponent = new Hero(opponentHp, opponentHp, 0, 0);
        playerHeroInfo.setHero(player);
        opponentHeroInfo.setHero(opponent);

        Image portrait = HeroInfo.spriteBuilder()
                .addImage("/standard_hero.png")
                .setStyle(HeroInfo.HeroStyle.BASE.toString())
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
                    .addParameter("pos", field.indexOf(selectedCard))
                    .addParameter("card_id", selectedCard.getCardInfo().getId())
                    .addParameter("target", "hero")
                    .build();

            GameApplication.getApplication().getClient().sendMessage(msg);
        });
    }
    public void onGameEnd(JSONObject json) {
        User.getInstance().setMoney(json.getInt("money"));
        switch (json.getString("result")) {
            case "win" -> {
                Animation animation = new CrackingAnimation(CrackingAnimation.Type.HERO, opponentHeroInfo.getPortrait());
                animation.addOnAnimationEndedListener(anim -> onGameEnd(true));
                animation.play();
            }
            case "defeat" -> {
                Animation animation = new CrackingAnimation(CrackingAnimation.Type.HERO, playerHeroInfo.getPortrait());
                animation.addOnAnimationEndedListener(anim -> onGameEnd(false));
                animation.play();
            }
        }
    }

    private void onGameEnd(boolean win) {
        FXMLLoader loader = new FXMLLoader(GameApplication.class.getResource("/fxml/game_end.fxml"));
        try {
            AnchorPane pane = loader.load();
            Text title = new Text();
            Font font = Font.loadFont(GameApplication.class.getResource("/fonts/ThaleahFat.ttf").toString(), 80);
            title.setFont(font);
            title.setX(675);
            title.setY(125);
            if (win) title.setText("You won!");
            else title.setText("You lost!");
            pane.getChildren().add(title);
            Scene scene = new Scene(pane);
            GameApplication.getApplication().getPrimaryStage().setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        resetController();
    }

    public void showMessage(String reason) {
        GameMessage.make(reason).show(root, 800, 625, 375);
    }

    public void playFieldFireAnimation() {
        Animation animation = new FieldEffectAnimation(FieldEffectAnimation.Type.FIRE_CIRCLE, fieldEffects);
        animation.play();
    }

    public void playAttackingAnimation(JSONObject json) {
        attacking = true;
        ImageView card;
        try {
            int pos = json.getInt("pos");
            int opponentPos = json.getInt("opponent_pos");
            Integer attackAnimationSrc = getIntParam(json, "attack_anim_src");
            Animation.OnAnimationEndedListener onAnimationEnded = (animation) -> {
                if (getIntParam(json, "punishment_src") != null) onPunishmentDamage(json);
                updateCards(json);
            };

            CardAttackingAnimation.CardEffect cardEffect = null;
            if (attackAnimationSrc != null && CardRepository.getCardTemplate(attackAnimationSrc).getActions().contains(CardRepository.Action.ATTACK_ADJACENT_CARDS)) {
                List<Integer> opponentPositions = getIntegersFromJSONArray(json, "opponent_anim_indexes");
                if (!opponentPositions.isEmpty()) {
                    cardEffect = new CardAttackingAnimation.CardEffect(
                            CardAttackingAnimation.CardEffect.Type.CLAW, opponentPositions, opponentField);
                }
                List<Integer> playerPositions = getIntegersFromJSONArray(json, "player_anim_indexes");
                if (!playerPositions.isEmpty()) {
                    cardEffect = new CardAttackingAnimation.CardEffect(
                            CardAttackingAnimation.CardEffect.Type.CLAW, playerPositions, field);
                }
            }

            if (json.getString("role").equals("attacker")) {
                CardAttackingAnimation cardAttackingAnimation = new CardAttackingAnimation(
                        field.get(pos).getAssociatedImageView(),
                        opponentField.get(opponentPos).getAssociatedImageView());
                cardAttackingAnimation.addOnAnimationEndedListener(onAnimationEnded);
                cardAttackingAnimation.addOnCardReturnedListener(() -> attacking = false);
                if (cardEffect != null) cardAttackingAnimation.addEffect(cardEffect);
                cardAttackingAnimation.play();
            }
            else {
                CardAttackingAnimation cardAttackingAnimation = new CardAttackingAnimation(
                        opponentField.get(opponentPos).getAssociatedImageView(),
                        field.get(pos).getAssociatedImageView());
                cardAttackingAnimation.addOnAnimationEndedListener(onAnimationEnded);
                cardAttackingAnimation.addOnCardReturnedListener(() -> attacking = false);
                if (cardEffect != null) cardAttackingAnimation.addEffect(cardEffect);
                cardAttackingAnimation.play();
            }
            return;
        } catch (JSONException ignored) {}
        try {
            int pos = json.getInt("field_pos");
            card = field.get(pos).getAssociatedImageView();
            CardAttackingAnimation cardAttackingAnimation = new CardAttackingAnimation(card, opponentHeroInfo.getPortrait());
            cardAttackingAnimation.play();
        } catch (JSONException e) {
            int pos = json.getInt("opponent_field_pos");
            card = opponentField.get(pos).getAssociatedImageView();
            CardAttackingAnimation cardAttackingAnimation = new CardAttackingAnimation(card, playerHeroInfo.getPortrait());
            cardAttackingAnimation.play();
        }
    }

    private List<Integer> getIntegersFromJSONArray(JSONObject jsonObject, String key) {
        List<Integer> positions = new ArrayList<>();
        try {
            positions = jsonObject.getJSONArray(key).toList()
                    .stream()
                    .map(obj -> (Integer) obj)
                    .collect(Collectors.toList());
        } catch (JSONException ignored) {}
        return positions;
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
            BattleCard stolen = field.remove(stolenPos);
            onCardDeselected(stolen.getAssociatedImageView());
            hBoxFieldCards.getChildren().remove(stolen.getAssociatedImageView());
        } catch (JSONException ignored) {}

        try {
            int gottenPos = json.getInt("gotten_pos");
            BattleCard card = getCard(json);
            Image sprite = CardImages.getPortrait(card.getCardInfo().getId());
            ImageView iv = new ImageView(sprite);
            card.associateImageView(iv);
            BattleCard gotten = opponentField.remove(gottenPos);
            hBoxOpponentFieldCards.getChildren().remove(gotten.getAssociatedImageView());
            field.add(card);
            hBoxFieldCards.getChildren().add(iv);
            setSelectionBehaviour(iv);
            setOnHoverListener(iv, "field");
        } catch (JSONException ignored) {}

        Integer handPos = getIntParam(json, "hand_pos");
        if (handPos != null) {
            BattleCard card = hand.get(handPos);
            card.setCost(json.getInt("cost"));
        }

        try {
            JSONArray frozenCards = json.getJSONArray("frozen_positions");
            freezeEnemyCards(frozenCards, field);
        } catch (JSONException ignored) {}
        try {
            JSONArray frozenCards = json.getJSONArray("opponent_frozen_positions");
            freezeEnemyCards(frozenCards, opponentField);
        } catch (JSONException ignored) {}
    }

    private void freezeEnemyCards(JSONArray frozenCards, List<BattleCard> field) {
        for (int i = 0; i < frozenCards.length(); i++) {
            BattleCard card = field.get(frozenCards.getInt(i));
            card.addStatus(CardRepository.Status.FROZEN);
            Animation freezeAnimation = new FreezeAnimation(FreezeAnimation.Type.FREEZING, card);
            freezeAnimation.play();
        }
    }

    private void setSelectionBehaviour(ImageView iv) {
        iv.setOnMouseClicked(mouseEvent -> {
            if (selectedCard != null) {
                BattleCard imgCard = getHandCardByImageView(iv);
                if (imgCard == null) imgCard = getFieldCardByImageView(iv);
                if (imgCard == selectedCard) onCardDeselected(iv);
                else onCardSelected(iv);
            } else onCardSelected(iv);
            mouseEvent.consume();
        });
    }

    private void checkShieldStatus(JSONObject json, BattleCard card) {
        String shieldStatus = getStringParam(json, "shield_status");
        if (shieldStatus != null) {
            switch (shieldStatus) {
                case "removed" -> {
                    card.removeStatus(CardRepository.Status.SHIELDED);
                    CardImages.removeShield(card);
                }
                case "given" -> {
                    card.addStatus(CardRepository.Status.SHIELDED);
                    CardImages.addShield(card.getAssociatedImageView());
                }
            }
        }
    }

    private void playCardCrackingAnimation(ImageView iv) {
        Animation animation = new CrackingAnimation(CrackingAnimation.Type.CARD, iv);
        animation.addOnAnimationEndedListener(anim -> {
            hBoxFieldCards.getChildren().remove(iv);
            hBoxOpponentFieldCards.getChildren().remove(iv);
        });
        animation.play();
    }

    private void applyChanges(JSONObject json, int pos, List<BattleCard> field) {
        BattleCard targeted = field.get(pos);
        Integer hp = getIntParam(json, "hp");
        Integer atk = getIntParam(json, "atk");
        String status = getStringParam(json, "card_status");
        checkShieldStatus(json, field.get(pos));
        applyChange(field, targeted, pos, hp, atk, status);
    }

    private void applyChange(List<BattleCard> field, BattleCard damaged, int pos, Integer hp, Integer atk, String status) {
        if (hp != null && hp <= 0) {
            field.remove(damaged);
            playCardCrackingAnimation(damaged.getAssociatedImageView());
        }
        else {
            BattleCard card = field.get(pos);
            if (hp != null) card.setHp(hp);
            if (atk != null) card.setAtk(atk);
            if (status != null) {
                switch (status) {
                    case "no_frozen" -> {
                        Animation unfreezeAnimation = new FreezeAnimation(FreezeAnimation.Type.UNFREEZING, field.get(pos));
                        unfreezeAnimation.play();
                        field.get(pos).removeStatus(CardRepository.Status.FROZEN);
                    }
                    case "FROZEN" -> {
                        if (!field.get(pos).hasStatus(CardRepository.Status.FROZEN)) {
                            field.get(pos).addStatus(CardRepository.Status.valueOf(status));
                            Animation freezeAnimation = new FreezeAnimation(FreezeAnimation.Type.FREEZING, field.get(pos));
                            freezeAnimation.play();
                        }
                    }
                    case "no_aligned" -> card.removeStatus(card.getCurrentAlignedStatus());
                    default -> card.addStatus(CardRepository.Status.valueOf(status));
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
                        .addParameter("hand_pos", hand.indexOf(selectedCard))
                        .addParameter("card_id", selectedCard.getCardInfo().getId());
                if (selectedCard.hasKeyWord(CardRepository.KeyWord.BATTLE_CRY)) msg.addParameter("card_action", "action");
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
                .addParameter("pos", handPos)
                .addParameter("card_id", hand.get(handPos).getCardInfo().getId());

        try {
            String cardAction = json.getString("card_action");
            builder.addParameter("card_action", cardAction);
            switch (cardAction) {
                case "target_enemy_card" -> builder.addParameter("opponent_pos",
                        String.valueOf(json.getInt("opponent_pos")));
            }
        } catch (JSONException ignored) {}

        GameApplication.getApplication().getClient().sendMessage(builder.build());
    }

    public void placeCard(JSONObject json) {
        int handPos = json.getInt("pos");
        BattleCard card = hand.get(handPos);
        addStatuses(card, json);
        ImageView cardIv = card.getAssociatedImageView();
        onCardDeselected(cardIv);

        int newMana = json.getInt("mana");
        manaBar.setMana(newMana, playerHeroInfo.getHero().getMaxMana());
        playerHeroInfo.getHero().setMana(newMana);

        hBoxHandCards.getChildren().remove(cardIv);
        hand.remove(card);
        field.add(card);
        if (card.hasStatus(CardRepository.Status.SHIELDED)) CardImages.addShield(cardIv);
        if (card.hasKeyWord(CardRepository.KeyWord.TAUNT)) CardImages.addTaunt(cardIv);
        hBoxFieldCards.getChildren().add(cardIv);
        setOnHoverListener(cardIv, "field");
    }

    public void updateOpponentHand(JSONObject json) {
        opponentHand.getChildren().clear();
        int handSize = json.getInt("opponent_hand_size");
        Image deckCover = BattleCard.spriteBuilder()
                .addImage("/assets/images/cards/card_cover.png")
                .setStyle(BattleCard.CardStyle.BASE.toString())
                .build();
        double deltaX = 40;
        for (int i = 0; i < handSize; i++) {
            ImageView iv = new ImageView(deckCover);
            AnchorPane.setLeftAnchor(iv, deltaX * i);
            opponentHand.getChildren().add(iv);
        }
    }

    public void onPunishmentDamage(JSONObject json) {
        String target = json.getString("target");
        int id = json.getInt("punishment_src");
        switch (target) {
            case "opponent" -> playPunishmentAnimation(opponentHeroInfo.getPortrait(), id);
            case "player" -> playPunishmentAnimation(playerHeroInfo.getPortrait(), id);
        }
    }

    private void playPunishmentAnimation(ImageView portrait, int id) {
        Animation animation = new PunishmentAnimation(portrait, id);
        animation.play();
    }

    public void changeOpponentMana(int newOpponentMana) {
        opponentHeroInfo.getHero().setMana(newOpponentMana);
        opponentManaBar.setMana(newOpponentMana, opponentHeroInfo.getHero().getMaxMana());
    }

    public void addOpponentCard(JSONObject json) {
        BattleCard card = getCard(json);
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

            BattleCard handCard = getHandCardByImageView(selectedCard.getAssociatedImageView());
            BattleCard selectedEnemyCard = getOpponentFieldCardByImageView(cardIv);

            if (handCard != null) {
                List<CardRepository.Action> enemyCardTargetedActions = CardRepository.Action.getEnemyCardTargetedActions();
                for (CardRepository.Action action : handCard.getCardInfo().getActions()) {
                    if (enemyCardTargetedActions.contains(action)) {
                        sendAttackingOnPlay(handCard, selectedEnemyCard);
                        mouseEvent.consume();
                        return;
                    }
                }
            }

            if (handCard != null) {
                mouseEvent.consume();
                return;
            }

            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.ROOM)
                    .setRoomAction(GameRoom.RoomAction.CHECK_CARD_TO_ATTACK)
                    .addParameter("pos", field.indexOf(selectedCard))
                    .addParameter("opponent_pos", opponentField.indexOf(selectedEnemyCard))
                    .addParameter("target", "card")
                    .addParameter("card_id", selectedCard.getCardInfo().getId())
                    .addParameter("opponent_card_id", selectedEnemyCard.getCardInfo().getId())
                    .build();

            GameApplication.getApplication().getClient().sendMessage(msg);
        });
        if (card.hasStatus(CardRepository.Status.SHIELDED)) CardImages.addShield(cardIv);
        if (card.hasKeyWord(CardRepository.KeyWord.TAUNT)) CardImages.addTaunt(cardIv);
        hBoxOpponentFieldCards.getChildren().add(cardIv);
    }

    private void sendAttackingOnPlay(BattleCard handCard, BattleCard opponentCard) {
        String msg = ServerMessage.builder()
                .setEntityToConnect(ServerMessage.Entity.ROOM)
                .setRoomAction(GameRoom.RoomAction.CHECK_CARD_PLAYED)
                .addParameter("hand_pos", hand.indexOf(handCard))
                .addParameter("opponent_pos", opponentField.indexOf(opponentCard))
                .addParameter("card_action", "target_enemy_card")
                .addParameter("card_id", handCard.getCardInfo().getId())
                .addParameter("opponent_card_id", opponentCard.getCardInfo().getId())
                .build();
        GameApplication.getApplication().getClient().sendMessage(msg);
    }

    public void updateCards(JSONObject json) {
        JSONArray changes = json.getJSONArray("stat_changes");
        JSONArray opponentChanges = json.getJSONArray("opponent_stat_changes");

        updateCards(changes, field);
        updateCards(opponentChanges, opponentField);
    }

    private void updateCards(JSONArray changes, List<BattleCard> field) {
        List<BattleCard> defeatedCards = new ArrayList<>();
        for (int i = 0; i < changes.length(); i++) {
            JSONObject cardChange = changes.getJSONObject(i);
            BattleCard card = applyChanges(field, cardChange);
            if (card.getHp() <= 0) defeatedCards.add(card);
        }
        removeCardsFrom(defeatedCards, field);
    }

    private void removeCardsFrom(List<BattleCard> cards, List<BattleCard> from) {
        for (BattleCard card : cards) {
            playCardCrackingAnimation(card.getAssociatedImageView());
        }
        from.removeAll(cards);
    }

    private BattleCard applyChanges(List<BattleCard> field, JSONObject cardChange) {
        int pos = cardChange.getInt("pos");
        BattleCard cardToChange = field.get(pos);
        Integer hp = getIntParam(cardChange, "hp");
        if (hp != null) cardToChange.setHp(hp);
        Integer atk = getIntParam(cardChange, "atk");
        if (atk != null) cardToChange.setAtk(atk);
        String status = getStringParam(cardChange, "card_status");
        if (status != null) {
            if (status.equals(CardRepository.Status.FROZEN.toString())) {
                cardToChange.addStatus(CardRepository.Status.FROZEN);
                Animation freezeAnimation = new FreezeAnimation(FreezeAnimation.Type.FREEZING, cardToChange);
                freezeAnimation.play();
            }
        }
        String alignedStatus = getStringParam(cardChange, "aligned_status");
        if (alignedStatus != null) {
            if (!alignedStatus.equals("no_aligned")) cardToChange.addStatus(CardRepository.Status.valueOf(alignedStatus));
            else cardToChange.removeStatus(cardToChange.getCurrentAlignedStatus());
        }
        checkShieldStatus(cardChange, cardToChange);
        return cardToChange;
    }

    private BattleCard getCard(JSONObject json) {
        BattleCard card = new BattleCard(
                json.getInt("id"),
                json.getInt("hp"),
                json.getInt("atk"),
                json.getInt("cost")
        );
        addStatuses(card, json);
        return card;
    }

    private void addStatuses(BattleCard card, JSONObject json) {
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
        BattleCard selected = getHandCardByImageView(card);
        if (selected == null) selected = getFieldCardByImageView(card);
        selectedCard = selected;
        selectCard(card);
    }

    private void onCardDeselected(ImageView card) {
        selectedCard = null;
        deselectCard(card);
    }

    private void selectCard(ImageView card) {
        Image sprite = CardImages.getSelectedPortraitWithStatusesAndEffects(selectedCard, List.of());

        card.setImage(sprite);
    }

    private void deselectCard(ImageView card) {
        BattleCard deselected = getHandCardByImageView(card);
        if (deselected == null) deselected = getFieldCardByImageView(card);
        if(deselected == null) return;
        Image sprite = CardImages.getPortraitWithStatusesAndEffects(deselected, List.of());

        card.setImage(sprite);
    }

    public void setHand(JSONArray cards) {
        ObservableList<Node> hBoxCardsChildren = hBoxHandCards.getChildren();
        for (int i = 0; i < cards.length(); i++) {
            JSONObject json = cards.getJSONObject(i);
            addCardToHand(json, hBoxCardsChildren);
        }
    }

    public void addCardsToHand(JSONArray cards) {
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

        BattleCard card = new BattleCard(cardInfo);
        card.setAtk(atk);
        card.setHp(hp);
        card.setCost(cost);
        card.associateImageView(img);
        setOnHoverListener(img, "hand");
        hand.add(card);

        setSelectionBehaviour(img);
        layoutCards.add(img);
    }

    private BattleCard getHandCardByImageView(ImageView iv) {
        for (BattleCard card : hand) {
            if (iv.equals(card.getAssociatedImageView())) return card;
        }
        return null;
    }

    private BattleCard getFieldCardByImageView(ImageView iv) {
        for (BattleCard card : field) {
            if (iv.equals(card.getAssociatedImageView())) return card;
        }
        return null;
    }

    private BattleCard getOpponentFieldCardByImageView(ImageView iv) {
        for (BattleCard card : opponentField) {
            if (iv.equals(card.getAssociatedImageView())) return card;
        }
        return null;
    }

    private void setOnHoverListener(ImageView iv, String place) {
        iv.hoverProperty().addListener(((observableValue, oldValue, isHovered) -> {
            BattleCard card = null;
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
        Hero player = playerHeroInfo.getHero();
        Hero opponent = opponentHeroInfo.getHero();
        if (mana != null) player.setMana(mana);
        if (maxMana != null) player.setMaxMana(maxMana);
        if (opponentMana != null) opponent.setMana(opponentMana);
        if (maxOpponentMana != null) opponent.setMaxMana(maxOpponentMana);
    }

    private void updateMana() {
        Hero player = playerHeroInfo.getHero();
        Hero opponent = opponentHeroInfo.getHero();
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
