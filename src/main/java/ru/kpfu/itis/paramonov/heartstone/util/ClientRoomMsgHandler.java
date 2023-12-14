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
            switch (GameRoom.RoomAction.valueOf(json.getString("room_action"))) {
                case GET_BACKGROUND -> {
                    String bg = json.getString("background");
                    if (BattlefieldController.getController() == null) {
                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                    }
                    BattlefieldController.getController().setBackground(bg);
                }
                case GET_HAND_AND_DECK -> {
                    if (BattlefieldController.getController() == null) {
                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                    }
                    BattlefieldController.getController().setHand(json.getJSONArray("hand"));
                    BattlefieldController.getController().setDeck(json.getJSONArray("deck"));
                }

                case END_TURN -> {
                    BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.RED);
                }
                case BEGIN_TURN -> {
                    if (BattlefieldController.getController() == null) {
                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                    }
                    BattlefieldController.getController().setActive(true);
                    BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.GREEN);
                    try {
                        BattlefieldController.getController().setDeck(json.getJSONArray("deck"));
                    } catch (JSONException ignored) {}
                }
                case DRAW_CARD -> {
                    if (BattlefieldController.getController() == null) {
                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                    }
                    if (json.getString("card_status").equals("drawn")) {
                        BattlefieldController.getController().addCardToHand(json.getJSONObject("card"));
                    }
                    BattlefieldController.getController().setDeck(json.getJSONArray("deck"));
                }
                case PLAY_CARD_OPPONENT -> {
                    BattlefieldController.getController().addOpponentCard(json);
                }
                case CARD_CARD_ATTACK -> {
                    BattlefieldController.getController().updateCards(json);
                }
            }
        } catch (JSONException ignored) {}
    }
}
