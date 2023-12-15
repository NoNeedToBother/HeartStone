package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONObject;

import java.util.HashMap;

public class ManaHelper {

    public static HashMap<String, Integer> increaseMana(JSONObject response, GameServer.Client activePlayer, GameServer.Client player1,
                                    HashMap<String, Integer> player1Mana, HashMap<String, Integer> player2Mana) {
        HashMap<String, Integer> mana;
        if (activePlayer.equals(player1)) mana = player1Mana;
        else mana = player2Mana;
        mana.put("mana", mana.get("maxMana") + 1);
        mana.put("maxMana", mana.get("maxMana") + 1);
        putManaInfo(response, mana);
        return mana;
    }

    private static void putManaInfo(JSONObject response, HashMap<String, Integer> mana) {
        response.put("mana", mana.get("mana"));
        response.put("maxMana", mana.get("maxMana"));
    }

    private static void putOpponentManaInfo(JSONObject response, HashMap<String, Integer> mana) {
        response.put("opponentMana", mana.get("mana"));
        response.put("maxOpponentMana", mana.get("maxMana"));
    }

    public static void getOpponentMana(JSONObject response, HashMap<String, Integer> mana) {
        response.put("room_action", GameRoom.RoomAction.GET_OPPONENT_MANA);
        putOpponentManaInfo(response, mana);
        response.put("status", "ok");
    }

}
