package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;

import java.util.List;

public class HeroHelper {
    private final static int INITIAL_HP = 25;

    public static int getInitialHp(List<Card> deck) {
        //in case of special cards
        return INITIAL_HP;
    }

    public static void putHpInfo(JSONObject response, int hp, int opponentHp) {
        response.put("hp", hp);
        response.put("opponent_hp", opponentHp);
    }


}
