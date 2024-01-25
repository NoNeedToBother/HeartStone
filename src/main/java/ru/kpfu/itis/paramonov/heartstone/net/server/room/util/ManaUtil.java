package ru.kpfu.itis.paramonov.heartstone.net.server.room.util;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.PlayerData;

public class ManaUtil {
    private static final int MAX_MANA = 10;
    public static Hero increaseMana(JSONObject response, PlayerData dataPlayer) {
        Hero hero = dataPlayer.getHero();
        int newMaxMana = hero.getMaxMana() + 1;
        if (newMaxMana >= MAX_MANA) newMaxMana = MAX_MANA;

        hero.setMana(newMaxMana);
        hero.setMaxMana(newMaxMana);

        putManaInfo(response, hero);
        return hero;
    }

    private static void putManaInfo(JSONObject response, Hero hero) {
        response.put("mana", hero.getMana());
        response.put("maxMana", hero.getMaxMana());
    }

    private static void putOpponentManaInfo(JSONObject response, Hero opponentHero) {
        response.put("opponentMana", opponentHero.getMana());
        response.put("maxOpponentMana", opponentHero.getMaxMana());
    }

    public static void getOpponentMana(JSONObject response, Hero opponentHero) {
        response.put("room_action", GameRoom.RoomAction.GET_OPPONENT_MANA);
        putOpponentManaInfo(response, opponentHero);
        response.put("status", "ok");
    }
}
