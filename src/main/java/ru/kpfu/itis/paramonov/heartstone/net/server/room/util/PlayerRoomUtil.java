package ru.kpfu.itis.paramonov.heartstone.net.server.room.util;

import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.ServerResponse;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.PlayerData;

import java.sql.SQLException;
import java.util.List;

public class PlayerRoomUtil {
    private final static int INITIAL_HP = 25;

    public static int getInitialHp(List<Card> deck) {
        //in case of special cards
        return INITIAL_HP;
    }

    public static void onHeroAttacked(ServerResponse responseAttacker, ServerResponse responseAttacked, Hero attackedHero,
                                      Card attacker, int attackerPos) {
        responseAttacker.putAction(GameRoom.RoomAction.CARD_HERO_ATTACK);
        responseAttacked.putAction(GameRoom.RoomAction.CARD_HERO_ATTACK);
        responseAttacker.putParameter("field_pos", attackerPos);
        responseAttacked.putParameter("opponent_field_pos", attackerPos);
        attacker.addStatus(CardRepository.Status.ATTACKED);
        int newHp = attackedHero.getHp() - attacker.getAtk();
        attackedHero.setHp(newHp);
        if (newHp > 0) {
            responseAttacker.putParameter("opponent_hp", newHp);
            responseAttacked.putParameter("hp", newHp);
        }
    }

    private static final int GOLD_FOR_WIN = 200;
    private static final int GOLD_FOR_DEFEAT = 50;

    public static void onHeroDefeated(ServerResponse responseWinner, ServerResponse responseDefeated, GameServer.Client winner,
                                      GameServer.Client defeated) {
        responseWinner.putAction(GameRoom.RoomAction.GAME_END);
        responseDefeated.putAction(GameRoom.RoomAction.GAME_END);
        UserService service = new UserService();
        User winnerUser = service.get(winner.getUserLogin());
        User defeatedUser = service.get(defeated.getUserLogin());
        int winnerMoney = winnerUser.money() + GOLD_FOR_WIN;
        int defeatedMoney = defeatedUser.money() + GOLD_FOR_DEFEAT;
        try {
            service.updateMoney(winnerUser.login(), winnerMoney);
            service.updateMoney(defeatedUser.login(), defeatedMoney);
        } catch (SQLException e) {
            responseWinner.putStatus("not_ok");
            responseDefeated.putStatus("not_ok");
            return;
        }
        responseWinner.putParameter("money", winnerMoney);
        responseDefeated.putParameter("money", defeatedMoney);
        responseWinner.putResult("win");
        responseDefeated.putResult("defeat");
    }

    public static void updateUsers(ServerResponse response, GameServer.Client winner, GameServer.Client defeated) throws SQLException {
        UserService service = new UserService();
        User winnerUser = service.get(winner.getUserLogin());
        User defeatedUser = service.get(defeated.getUserLogin());
        int winnerMoney = winnerUser.money() + GOLD_FOR_WIN;
        int defeatedMoney = defeatedUser.money() + GOLD_FOR_DEFEAT;
        service.updateMoney(winnerUser.login(), winnerMoney);
        service.updateMoney(defeatedUser.login(), defeatedMoney);
        response.putParameter("money", winnerMoney);
    }
    public static void dealDamageOnNoCard(PlayerData playerData, ServerResponse responsePlayerDamaged,
                                          ServerResponse responsePlayerOther) {
        responsePlayerDamaged.putAction(GameRoom.RoomAction.CHANGE_HP);
        responsePlayerOther.putAction(GameRoom.RoomAction.CHANGE_HP);
        Hero hero = playerData.getHero();
        int dmg = playerData.getBurntCardDamage();
        int newHp = hero.getHp() - dmg;
        hero.setHp(newHp);
        responsePlayerDamaged.putParameter("hp", hero.getHp());
        responsePlayerDamaged.putReason("no_card");
        responsePlayerDamaged.putParameter("dmg", dmg);
        responsePlayerOther.putParameter("opponent_hp", hero.getHp());
    }
}
