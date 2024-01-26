package ru.kpfu.itis.paramonov.heartstone.net;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;

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
        jsonResponse.put(parameter, value.toString());
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

    @Override
    public String toString() {
        return jsonResponse.toString();
    }
}
