package ru.kpfu.itis.paramonov.heartstone.util;

import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

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
                String bg = json.getString("background");
                BattlefieldController.getController().setBackground(bg);
            }
            case GET_INITIAL_INFO -> {
                BattlefieldController.getController().setHand(json.getJSONArray("hand"));
                BattlefieldController.getController().setDeck(json.getJSONArray("deck"));
            }
            case END_TURN -> {
                BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.RED);
            }
            case BEGIN_TURN -> {
                BattlefieldController.getController().setActive(true);
                BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.GREEN);
                try {
                    BattlefieldController.getController().setDeck(json.getJSONArray("deck"));
                } catch (JSONException ignored) {}
                BattlefieldController.getController().setMana(json);
            }
            case DRAW_CARD -> {
                if (json.getString("card_status").equals("drawn")) {
                    BattlefieldController.getController().addCardToHand(json.getJSONObject("card"));
                }
                BattlefieldController.getController().setDeck(json.getJSONArray("deck"));
            }
            case PLAY_CARD_OPPONENT -> {
                BattlefieldController.getController().addOpponentCard(json);
                BattlefieldController.getController().changeOpponentMana(json.getInt("opponent_mana"));
            }
            case CARD_CARD_ATTACK -> BattlefieldController.getController().updateCards(json);
            case GET_OPPONENT_MANA -> {
                BattlefieldController.getController().setMana(json);
            }
            case CHECK_CARD_PLAYED -> {
                if (json.getString("status").equals("ok")) {
                    BattlefieldController.getController().placeCard(json.getInt("hand_pos"));
                } else {
                    System.out.println(json.getString("reason"));
                }
            }
        }
    }

    private void loadBattlefieldIfNecessary() {
        if (BattlefieldController.getController() == null) {
            GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
        }
    }
}
