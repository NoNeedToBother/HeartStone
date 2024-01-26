package ru.kpfu.itis.paramonov.heartstone.net;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.util.CardUtil;

import java.util.List;

public class ServerResponse {
    private final JSONObject jsonResponse = new JSONObject();

    public void putStatus(String status) {
        jsonResponse.put("status", status);
    }

    public void putReason(String reason) {
        jsonResponse.put("reason", reason);
    }

    public void putAction(GameRoom.RoomAction roomAction) {
        jsonResponse.put("room_action", roomAction.toString());
    }

    public void putAction(ServerMessage.ServerAction serverAction) {
        jsonResponse.put("server_action", serverAction.toString());
    }

    public void putParameter(String parameter, Object value) {
        jsonResponse.put(parameter, value);
    }

    public void putIntegers(String parameter, List<Integer> integers) {
        JSONArray jsonArray = new JSONArray();
        for (Integer integer : integers) {
            jsonArray.put(integer);
        }
        jsonResponse.put(parameter, jsonArray);
    }

    public void putUserInfo(User user) {
        jsonResponse.put("login", user.login());
        jsonResponse.put("deck", user.deck());
        jsonResponse.put("cards", user.cards());
        jsonResponse.put("money", user.money());
    }

    public void putManaInfo(Hero hero) {
        jsonResponse.put("mana", hero.getMana());
        jsonResponse.put("maxMana", hero.getMaxMana());
    }

    public void putOpponentManaInfo(Hero opponentHero) {
        jsonResponse.put("opponentMana", opponentHero.getMana());
        jsonResponse.put("maxOpponentMana", opponentHero.getMaxMana());
    }

    public void putHpInfo(int hp, int opponentHp) {
        jsonResponse.put("hp", hp);
        jsonResponse.put("opponent_hp", opponentHp);
    }

    public void putStatuses(List<CardRepository.Status> statuses) {
        JSONArray responseStatuses = new JSONArray();
        for (CardRepository.Status status : statuses) {
            responseStatuses.put(status.toString());
        }
        jsonResponse.put("statuses", responseStatuses);
    }

    public void putCardStatsAndId(Card card) {
        jsonResponse.put("hp", card.getHp());
        jsonResponse.put("atk", card.getAtk());
        jsonResponse.put("cost", card.getCost());
        jsonResponse.put("id", card.getCardInfo().getId());
    }

    public void putFieldChanges(List<Card> field, List<Card> opponentField,
                                List<Integer> positions, List<Integer> opponentPositions) {
        JSONArray changes = CardUtil.getFieldChanges(field, positions);
        jsonResponse.put("stat_changes", changes);
        JSONArray opponentChanges = CardUtil.getFieldChanges(opponentField, opponentPositions);
        jsonResponse.put("opponent_stat_changes", opponentChanges);
    }

    public void putResult(String result) {
        jsonResponse.put("result", result);
    }

    public String getStatus() {
        return jsonResponse.getString("status");
    }

    @Override
    public String toString() {
        return jsonResponse.toString();
    }
}
