package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.sql.SQLException;
import java.util.List;

public class PlayerRoomUtil {
    private final static int INITIAL_HP = 25;

    public static int getInitialHp(List<Card> deck) {
        //in case of special cards
        return INITIAL_HP;
    }

    public static void putHpInfo(JSONObject response, int hp, int opponentHp) {
        response.put("hp", hp);
        response.put("opponent_hp", opponentHp);
    }

    public static void onHeroAttacked(JSONObject responseAttacker, JSONObject responseAttacked, Hero attackedHero, Card attacker, int attackerPos) {
        responseAttacker.put("room_action", GameRoom.RoomAction.CARD_HERO_ATTACK.toString());
        responseAttacked.put("room_action", GameRoom.RoomAction.CARD_HERO_ATTACK.toString());
        responseAttacker.put("field_pos", attackerPos);
        responseAttacked.put("opponent_field_pos", attackerPos);
        attacker.addStatus(CardRepository.Status.ATTACKED);
        int newHp = attackedHero.getHp() - attacker.getAtk();
        attackedHero.setHp(newHp);
        if (newHp > 0) {
            responseAttacker.put("opponent_hp", newHp);
            responseAttacked.put("hp", newHp);
        }
    }

    private static final int GOLD_FOR_WIN = 200;
    private static final int GOLD_FOR_DEFEAT = 50;

    public static void onHeroDefeated(JSONObject responseWinner, JSONObject responseDefeated, GameServer.Client winner,
                                      GameServer.Client defeated) {
        responseWinner.put("room_action", GameRoom.RoomAction.GAME_END.toString());
        responseDefeated.put("room_action", GameRoom.RoomAction.GAME_END.toString());
        UserService service = new UserService();
        User winnerUser = service.get(winner.getUserLogin());
        User defeatedUser = service.get(defeated.getUserLogin());
        int winnerMoney = winnerUser.money() + GOLD_FOR_WIN;
        int defeatedMoney = defeatedUser.money() + GOLD_FOR_DEFEAT;
        try {
            service.updateMoney(winnerUser.login(), winnerMoney);
            service.updateMoney(defeatedUser.login(), defeatedMoney);
        } catch (SQLException e) {
            responseWinner.put("status", "not_ok");
            responseDefeated.put("status", "not_ok");
            return;
        }
        responseWinner.put("money", winnerMoney);
        responseDefeated.put("money", defeatedMoney);
        responseWinner.put("result", "win");
        responseDefeated.put("result", "defeat");
    }

    public static void updateUsers(JSONObject response, GameServer.Client winner, GameServer.Client defeated) throws SQLException {
        UserService service = new UserService();
        User winnerUser = service.get(winner.getUserLogin());
        User defeatedUser = service.get(defeated.getUserLogin());
        int winnerMoney = winnerUser.money() + GOLD_FOR_WIN;
        int defeatedMoney = defeatedUser.money() + GOLD_FOR_DEFEAT;
        service.updateMoney(winnerUser.login(), winnerMoney);
        service.updateMoney(defeatedUser.login(), defeatedMoney);
        response.put("money", winnerMoney);
    }
    public static void dealDamageOnNoCard(GameServer.Client client, GameServer.Client player1, int player1Dmg, int player2Dmg,
                                          Hero player1Hero, Hero player2Hero, JSONObject responsePlayer1, JSONObject responsePlayer2) {
        responsePlayer1.put("room_action", GameRoom.RoomAction.CHANGE_HP);
        responsePlayer2.put("room_action", GameRoom.RoomAction.CHANGE_HP);
        if (client.equals(player1)) {
            int newHp = player1Hero.getHp() - player1Dmg;
            responsePlayer1.put("hp", newHp);
            responsePlayer1.put("reason", "no_card");
            responsePlayer1.put("dmg", player1Dmg);
            responsePlayer2.put("opponent_hp", newHp);
            player1Hero.setHp(newHp);
        } else {
            int newHp = player2Hero.getHp() - player2Dmg;
            responsePlayer2.put("hp", newHp);
            responsePlayer2.put("reason", "no_card");
            responsePlayer2.put("dmg", player2Dmg);
            responsePlayer1.put("opponent_hp", newHp);
            player2Hero.setHp(newHp);
        }
    }
}
