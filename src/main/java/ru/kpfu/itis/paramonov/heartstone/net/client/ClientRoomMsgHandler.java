package ru.kpfu.itis.paramonov.heartstone.net.client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

public class ClientRoomMsgHandler {
    public void handle(String response) {
        JSONObject json = new JSONObject(response);
        try {
            json.getString("room_action");
        } catch (JSONException e) {
            return;
        }
        System.out.println(response);
        loadBattlefieldIfNecessary();
        switch (GameRoom.RoomAction.valueOf(json.getString("room_action"))) {
            case GET_INITIAL_INFO -> {
                String bg = json.getString("background");
                BattlefieldController.getController().setBackground(bg);
                BattlefieldController.getController().setHand(json.getJSONArray("hand"));
                BattlefieldController.getController().setDeckSize(json.getInt("deck_size"));
                BattlefieldController.getController().setHeroes(json);
                BattlefieldController.getController().updateOpponentHand(json);
            }
            case END_TURN -> BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.RED);
            case BEGIN_TURN -> {
                BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.GREEN);
                try {
                    BattlefieldController.getController().setDeckSize(json.getInt("deck_size"));
                } catch (JSONException ignored) {}
                BattlefieldController.getController().setMana(json);
            }
            case DRAW_CARD -> {
                if (json.getString("card_status").equals("drawn")) {
                    BattlefieldController.getController().addCardToHand(json.getJSONObject("card"));
                }
                BattlefieldController.getController().setDeckSize(json.getInt("deck_size"));
            }
            case DRAW_CARD_OPPONENT -> BattlefieldController.getController().updateOpponentHand(json);
            case PLAY_CARD_OPPONENT -> {
                BattlefieldController.getController().addOpponentCard(json);
                BattlefieldController.getController().changeOpponentMana(json.getInt("opponent_mana"));
                BattlefieldController.getController().updateOpponentHand(json);
            }
            case PLAY_CARD -> BattlefieldController.getController().placeCard(json);
            case CARD_CARD_ATTACK -> {
                BattlefieldController.getController().playAttackingAnimation(json);
                try {
                    json.getInt("hp");
                    BattlefieldController.getController().updateHp(json);
                } catch (JSONException ignored) {}
            }
            case GET_OPPONENT_MANA -> BattlefieldController.getController().setMana(json);
            case CHECK_CARD_PLAYED -> {
                if (json.getString("status").equals("ok")) BattlefieldController.getController().playCard(json);
                else BattlefieldController.getController().showMessage(json.getString("reason"));
            }
            case CHECK_CARD_TO_ATTACK -> {
                if (json.getString("status").equals("not_ok")) {
                    try {
                        String reason = json.getString("reason");
                        BattlefieldController.getController().showMessage(reason);
                    } catch (JSONException e) {
                        BattlefieldController.getController().showMessage("Card cannot attack");
                    }
                }
            }
            case CARD_HERO_ATTACK -> {
                BattlefieldController.getController().updateHp(json);
                BattlefieldController.getController().playAttackingAnimation(json);
            }
            case GAME_END -> BattlefieldController.getController().onGameEnd(json);
            case CHANGE_HP -> {
                try {
                    json.getString("reason");
                    BattlefieldController.getController().showMessage("You have no card \nand took " +
                            json.getInt("dmg") + " damage");
                } catch (JSONException ignored) {}
                BattlefieldController.getController().updateHp(json);
            }
            case GET_CHANGE -> {
                try {
                    json.getJSONArray("stat_changes");
                    BattlefieldController.getController().updateCards(json);
                } catch (JSONException e) {
                    BattlefieldController.getController().applyChange(json);
                }
                try {
                    String anim = json.getString("anim");
                    switch (anim) {
                        case "field_fire" -> BattlefieldController.getController().playFieldFireAnimation();
                    }
                } catch (JSONException ignored) {}
            }
            case ADD_CARDS_TO_HAND -> {
                try {
                    JSONArray gottenCards = json.getJSONArray("gotten_cards");
                    BattlefieldController.getController().addCardsToHand(gottenCards);
                } catch (JSONException ignored) {
                    BattlefieldController.getController().updateOpponentHand(json);
                }
            }
            case TIMER_UPDATE -> BattlefieldController.getController().handleTimer(json);
        }
    }

    private void loadBattlefieldIfNecessary() {
        if (BattlefieldController.getController() == null) {
            GameApplication.getApplication().loadScene("/battlefield.fxml");
        }
    }
}
