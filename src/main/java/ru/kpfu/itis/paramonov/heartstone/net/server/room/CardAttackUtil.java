package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CardAttackUtil {
    public static boolean checkCardToAttack(GameServer.Client player, GameServer.Client activePlayer,
                                            JSONObject response, Card attacker, int pos, String target) {
        response.put("room_action", GameRoom.RoomAction.CHECK_CARD_TO_ATTACK);
        response.put("pos", pos);
        response.put("target", target);
        boolean canAttack = player.equals(activePlayer);
        for (CardRepository.Status status : attacker.getStatuses()) {
            if (CardRepository.Status.getCardAttackRestrictionStatuses().contains(status)) {
                canAttack = false;
                break;
            }
        }
        return canAttack;
    }

    public static void checkCardToAttack(GameServer.Client client, GameServer.Client activePlayer, PlayerData opponentData,
                                         JSONObject response, Card attacker, int pos, int opponentPos, String target) {
        boolean res = checkCardToAttack(client, activePlayer, response, attacker, pos, target);
        if (res) {
            if (!attacker.hasAction(CardRepository.Action.IGNORE_TAUNT)) {
                res = checkTaunts(opponentData, opponentPos);
                if (!res) response.put("reason", "You must attack card with taunt");
            }
        }
        if (res) response.put("status", "ok");
        else response.put("status", "not_ok");
        response.put("opponent_pos", opponentPos);
    }

    private static boolean checkTaunts(PlayerData playerData, int pos) {
        List<Integer> tauntCards = checkTaunts(playerData);
        if (tauntCards.size() == 0) return true;
        return checkTaunts(playerData).contains(pos);
    }

    public static List<Integer> checkTaunts(PlayerData playerData) {
        List<Integer> tauntCardPositions = new ArrayList<>();
        for (Card card : playerData.getField()) {
            if (card.hasKeyWord(CardRepository.KeyWord.TAUNT)) {
                tauntCardPositions.add(playerData.getField().indexOf(card));
            }
        }
        return tauntCardPositions;
    }

    public static void decreaseHpOnDirectAttack(Card attacker, Card attacked) {
        attacked.decreaseHp(attacker.getAtk());
        attacker.decreaseHp(attacked.getAtk());
    }

    public static void checkAttackSpecialEffects(Card attacker, Card attacked, List<Integer> attackerIndexes, List<Integer> attackedIndexes,
                                                 JSONObject attackerResponse, JSONObject attackedResponse,
                                                 GameServer.Client attackerPlayer, GameRoom room) {
        GameServer.Client attackedPlayer = room.getOtherPlayer(attackerPlayer);
        Hero attackerHero = room.getPlayerData(attackerPlayer).getHero();
        Hero attackedHero = room.getPlayerData(attackedPlayer).getHero();
        List<Card> attackerField = room.getPlayerData(attackerPlayer).getField();
        List<Card> attackedField = room.getPlayerData(attackedPlayer).getField();
        if (attacked.hasKeyWord(CardRepository.KeyWord.PUNISHMENT) && attacked.getHp() > 0) {
            if (attacked.getCardInfo().getId() == CardRepository.CardTemplate.TheRock.getId()) {
                dealHeroDamageOnPunishment(attackerHero, attackedHero, attacked.getAtk(), attackerResponse, attackedResponse,
                        attackerPlayer, attackedPlayer);
                attacked.setAtk(attacked.getAtk() + attacked.getCardInfo().getAtkIncrease());
                attacked.increaseMaxHp(attacked.getCardInfo().getHpIncrease());
            }
            if (attacked.hasAction(CardRepository.Action.DAMAGE_ENEMY_HERO_ON_DMG)) {
                dealHeroDamageOnPunishment(attackerHero, attackedHero, attacked.getCardInfo().getHeroDamage(),
                        attackerResponse, attackedResponse, attackerPlayer, attackedPlayer);
            }
            attackedResponse.put("punishment_src", attacked.getCardInfo().getId());
            attackerResponse.put("punishment_src", attacked.getCardInfo().getId());
            attackedResponse.put("target", "opponent");
            attackerResponse.put("target", "player");
        }
        if (attacker.hasKeyWord(CardRepository.KeyWord.ALIGNMENT)) {
            CardRepository.Status previous = attacked.getCurrentAlignedStatus();
            CardRepository.Status alignment = AlignmentUtil.getAlignment(attacker);
            if (previous != null)
                AlignmentUtil.onAlignedStatusApply(attacker, attacked, attackedField, attackedIndexes, previous, alignment, attackerPlayer, room);
            AlignmentUtil.addAlignment(attacked, alignment);
        }
        if (attacker.hasAction(CardRepository.Action.ATTACK_ADJACENT_CARDS)) {
            JSONArray effects = null;
            boolean isCardMutantCrab = attacker.getCardInfo().getId() == CardRepository.CardTemplate.MutantCrab.getId();
            boolean isCardPirateParrot = attacker.getCardInfo().getId() == CardRepository.CardTemplate.PirateParrot.getId();
            if (isCardMutantCrab || isCardPirateParrot) effects = new JSONArray();
            decreaseHpFromNeighbourCards(attacker.getAtk(), attackedField.indexOf(attacked), attackedField, attackedIndexes, effects);
            if (isCardMutantCrab || isCardPirateParrot) {
                attackerResponse.put("opponent_anim_indexes", effects);
                attackerResponse.put("attack_anim_src", attacker.getCardInfo().getId());
                attackedResponse.put("player_anim_indexes", effects);
                attackerResponse.put("attack_anim_src", attacker.getCardInfo().getId());
            }
        }
    }
    public static void decreaseHpFromNeighbourCards(int hpDecrease, int pos, List<Card> field, List<Integer> indexes, JSONArray effects) {
        List<Integer> positions = List.of(pos - 1, pos + 1);
        for (Integer neighbourPos : positions) {
            try {
                Card card = field.get(neighbourPos);
                card.decreaseHp(hpDecrease);
                if (effects != null) effects.put(neighbourPos);
                indexes.add(neighbourPos);
            } catch (IndexOutOfBoundsException ignored) {}
        }
    }

    private static void dealHeroDamageOnPunishment(Hero attackerHero, Hero attackedHero, int punishmentDamage,
                                                   JSONObject attackerResponse, JSONObject attackedResponse,
                                                   GameServer.Client attackerPlayer, GameServer.Client attackedPlayer) {
        attackerHero.setHp(attackerHero.getHp() - punishmentDamage);
        if (attackerHero.getHp() <= 0) {
            JSONObject responseWinner = new JSONObject();
            JSONObject responseDefeated = new JSONObject();
            PlayerRoomUtil.onHeroDefeated(responseWinner, responseDefeated, attackedPlayer, attackerPlayer);
        }
        PlayerRoomUtil.putHpInfo(attackerResponse, attackerHero.getHp(), attackedHero.getHp());
        PlayerRoomUtil.putHpInfo(attackedResponse, attackedHero.getHp(), attackerHero.getHp());
    }

    public static void putCardCardAttackAnimationInfo(JSONObject response, int pos, int opponentPos, String role) {
        response.put("room_action", GameRoom.RoomAction.CARD_CARD_ATTACK.toString());
        response.put("status", "ok");
        response.put("pos", pos);
        response.put("opponent_pos", opponentPos);
        response.put("role", role);
    }
}
