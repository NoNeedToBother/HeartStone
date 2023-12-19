package ru.kpfu.itis.paramonov.heartstone.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

import java.util.Base64;

public class ClientRoomMsgHandler {
    public void handle(String response) {
        JSONObject json = new JSONObject(response);
        try {
            json.getString("room_action");
        } catch (JSONException e) {
            return;
        }
        loadBattlefieldIfNecessary();
        switch (GameRoom.RoomAction.valueOf(json.getString("room_action"))) {
            case GET_BACKGROUND -> {
                loadBattlefieldIfNecessary();
                String bg = json.getString("background");
                BattlefieldController.getController().setBackground(bg);
            }
            case GET_INITIAL_INFO -> {
                BattlefieldController.getController().setHand(json.getJSONArray("hand"));
                BattlefieldController.getController().setDeckSize(json.getInt("deck_size"));
                BattlefieldController.getController().setHeroes(json);
            }
            case END_TURN -> {
                BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.RED);
            }
            case BEGIN_TURN -> {
                BattlefieldController.getController().setActive(true);
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
            case PLAY_CARD_OPPONENT -> {
                BattlefieldController.getController().addOpponentCard(json);
                BattlefieldController.getController().changeOpponentMana(json.getInt("opponent_mana"));
            }
            case CARD_CARD_ATTACK -> {
                BattlefieldController.getController().playAttackingAnimation(json);
                BattlefieldController.getController().updateCards(json);
            }
            case GET_OPPONENT_MANA -> {
                BattlefieldController.getController().setMana(json);
            }
            case CHECK_CARD_PLAYED -> {
                if (json.getString("status").equals("ok")) {
                    BattlefieldController.getController().placeCard(json);
                } else {
                    BattlefieldController.getController().showMessage(json.getString("reason"));
                }
            }
            case CHECK_CARD_TO_ATTACK -> {
                if (json.getString("status").equals("ok")) {
                    switch (json.getString("target")) {
                        case "hero" -> BattlefieldController.getController().attack(json.getInt("pos"), null, "hero");
                        case "card" -> BattlefieldController.getController().attack(json.getInt("pos"), json.getInt("opponent_pos"), "card");
                    }
                } else {
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
                } catch (JSONException e) {}
                BattlefieldController.getController().updateHp(json);
            }
            case GET_CHANGE -> {
                try {
                    JSONArray msg = json.getJSONArray("stat_changes");
                    BattlefieldController.getController().updateCards(json);
                } catch (JSONException e) {
                    BattlefieldController.getController().applyChange(json);
                }
            }
            case TIMER_UPDATE -> {
                BattlefieldController.getController().handleTimer(json);
            }
        }
    }

    private void loadBattlefieldIfNecessary() {
        if (BattlefieldController.getController() == null) {
            GameApplication.getApplication().loadScene("/battlefield.fxml");
        }
    }
}
