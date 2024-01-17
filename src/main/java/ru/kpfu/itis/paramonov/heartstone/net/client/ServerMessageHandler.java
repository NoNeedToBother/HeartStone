package ru.kpfu.itis.paramonov.heartstone.net.client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.*;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;

public class ServerMessageHandler {
    public void handle(String response) {
        JSONObject json = new JSONObject(response);
        try {
            json.getString("server_action");
        } catch (JSONException e) {
            return;
        }
        switch (ServerMessage.ServerAction.valueOf(json.getString("server_action"))) {
            case LOGIN, REGISTER -> {
                if (checkStatus(json)) {
                    setGameUser(json);
                    GameApplication.getApplication().loadScene("/main_menu.fxml");
                } else {
                    if (ServerMessage.ServerAction.valueOf(json.getString("server_action")).equals(ServerMessage.ServerAction.LOGIN)) {
                        LoginController.getController().showMessage("Unable to login, try again later", 1000);
                    }
                    else {
                        RegisterController.getController().showMessage("User with this login exists, try again later", 1000);
                    }
                }
            }
            case OPEN_1_PACK, OPEN_5_PACKS -> {
                if (checkStatus(json)) {
                    User.getInstance().setMoney(json.getInt("money"));
                    Integer cardId = null;
                    JSONArray cardIds = null;
                    try {
                        cardId = json.getInt("card_id");
                    } catch (JSONException ignored) {}
                    try {
                        cardIds = json.getJSONArray("card_ids");
                    } catch (JSONException ignored) {}
                    PacksController.getController().playOpeningAnimation(cardId, cardIds);
                    PacksController.getController().updateUserInfo(json);
                    try {
                        MainMenuController.getController().setMoney(json.getInt("money"));
                    } catch (NullPointerException ignored) {}
                } else {
                    PacksController.getController().showMessage(json.getString("reason"), 1000);
                }
            }
        }
    }

    private boolean checkStatus(JSONObject json) {
        return json.getString("status").equalsIgnoreCase("OK");
    }

    private void setGameUser(JSONObject json) {
        User user = User.getInstance();
        user.setLogin(json.getString("login"));
        user.setDeck(CardRepository.getCardsById(json.getString("deck")));
        user.setCards(CardRepository.getCardsById(json.getString("cards")));
        user.setMoney(json.getInt("money"));
    }
}
