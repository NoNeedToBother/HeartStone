package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;

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

    public static void onHeroAttacked(JSONObject responseAttacker, JSONObject responseAttacked, Hero attackedHero, Card attacker) {
        responseAttacker.put("room_action", GameRoom.RoomAction.CARD_HERO_ATTACK.toString());
        responseAttacked.put("room_action", GameRoom.RoomAction.CARD_HERO_ATTACK.toString());
        int newHp = attackedHero.getHp() - attacker.getAtk();
        if (newHp <= 0) onHeroDefeated();
        else {
            responseAttacker.put("opponent_hp", newHp);
            responseAttacked.put("hp", newHp);
        }
    }

    private static void onHeroDefeated() {
        //Not yet implemented
    }
}
