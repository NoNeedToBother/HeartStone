package ru.kpfu.itis.paramonov.heartstone.util;

import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;
import ru.kpfu.itis.paramonov.heartstone.controller.PacksController;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;

public class ClientServerMsgHandler {
    public void handle(String response) {
        JSONObject json = new JSONObject(response);
        try {
            json.getString("server_action");
        } catch (JSONException e) {
            return;
        }
        switch (ServerMessage.ServerAction.valueOf(json.getString("server_action"))) {
            case CONNECT -> {
                if (checkStatus(json))
                    if (BattlefieldController.getController() == null) {
                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                    }
            }
            case LOGIN, REGISTER -> {
                if (checkStatus(json)) {
                    setGameUser(json);
                    GameApplication.getApplication().loadScene("/fxml/main_menu.fxml");
                }
            }
            case OPEN_1_PACK -> {
                if (checkStatus(json)) {
                    PacksController.getController().playOpeningAnimation();
                }
            }
            case OPEN_5_PACKS -> {
                if (checkStatus(json)) {
                    PacksController.getController().playOpeningAnimation();
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
