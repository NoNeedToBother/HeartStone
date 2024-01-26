package ru.kpfu.itis.paramonov.heartstone.net.server.room.util;

import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.ServerResponse;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.PlayerData;

public class ManaUtil {
    private static final int MAX_MANA = 10;
    public static Hero increaseMana(ServerResponse response, PlayerData dataPlayer) {
        Hero hero = dataPlayer.getHero();
        int newMaxMana = hero.getMaxMana() + 1;
        if (newMaxMana >= MAX_MANA) newMaxMana = MAX_MANA;

        hero.setMana(newMaxMana);
        hero.setMaxMana(newMaxMana);

        response.putManaInfo(hero);
        return hero;
    }

    public static void getOpponentMana(ServerResponse response, Hero opponentHero) {
        response.putAction(GameRoom.RoomAction.GET_OPPONENT_MANA);
        response.putOpponentManaInfo(opponentHero);
        response.putStatus("ok");
    }
}
