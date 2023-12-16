package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;

public class ManaHelper {

    public static Hero increaseMana(JSONObject response, GameServer.Client activePlayer, GameServer.Client player1,
                                    Hero player1Hero, Hero player2Hero) {
        Hero hero;
        if (activePlayer.equals(player1)) hero = player1Hero;
        else hero = player2Hero;
        int newMaxMana = hero.getMaxMana() + 1;

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
