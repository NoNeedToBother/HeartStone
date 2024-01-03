package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CardAttackUtil {
    public static boolean checkCardToAttack(GameServer.Client client, GameServer.Client activePlayer,
                                            HashMap<String, List<Card>> opponentCards, JSONObject response,
                                            Card attacker, int pos, String target) {
        response.put("room_action", GameRoom.RoomAction.CHECK_CARD_TO_ATTACK);
        response.put("pos", pos);
        response.put("target", target);
        boolean canAttack = client.equals(activePlayer);
        for (CardRepository.Status status : attacker.getStatuses()) {
            if (CardRepository.Status.getCardAttackRestrictionStatuses().contains(status)) {
                canAttack = false;
                break;
            }
        }
        return canAttack;
    }

    public static void checkCardToAttack(GameServer.Client client, GameServer.Client activePlayer, HashMap<String, List<Card>> opponentCards,
                                         JSONObject response, Card attacker, int pos, int opponentPos, String target) {
        boolean res = checkCardToAttack(client, activePlayer, opponentCards, response, attacker, pos, target);
        if (res) {
            if (!attacker.getCardInfo().getActions().contains(CardRepository.Action.IGNORE_TAUNT)) {
                res = checkTaunts(opponentCards, opponentPos);
                if (!res) response.put("reason", "You must attack card with taunt");
            }
        }
        if (res) response.put("status", "ok");
        else response.put("status", "not_ok");
        response.put("opponent_pos", opponentPos);
    }

    private static boolean checkTaunts(HashMap<String, List<Card>> cards, int pos) {
        List<Integer> tauntCards = checkTaunts(cards);
        if (tauntCards.size() == 0) return true;
        return checkTaunts(cards).contains(pos);
    }

    public static List<Integer> checkTaunts(HashMap<String, List<Card>> cards) {
        List<Integer> tauntCardPositions = new ArrayList<>();
        for (Card card : cards.get("field")) {
            if (card.getCardInfo().getKeyWords().contains(CardRepository.KeyWord.TAUNT)) {
                tauntCardPositions.add(cards.get("field").indexOf(card));
            }
        }
        return tauntCardPositions;
    }

    public static void decreaseHpOnDirectAttack(Card attacker, Card attacked) {
        attacked.decreaseHp(attacker.getAtk());
        attacker.decreaseHp(attacked.getAtk());
    }

    public static void checkAttackSpecialEffects(Card attacker, Card attacked, List<Card> attackerField,
                                                 List<Card> attackedField, List<Integer> attackerIndexes,
                                                 List<Integer> attackedIndexes, Hero attackerHero, Hero attackedHero,
                                                 JSONObject attackerResponse, JSONObject attackedResponse,
                                                 GameServer.Client attackerPlayer, GameServer.Client attackedPlayer, GameRoom room) {
        if (attacked.getCardInfo().getKeyWords().contains(CardRepository.KeyWord.PUNISHMENT)) {
            if (attacked.getCardInfo().getId() == CardRepository.CardTemplate.TheRock.getId() && attacked.getHp() > 0) {
                dealHeroDamageOnPunishment(attackerHero, attackedHero, attacked.getAtk(), attackerResponse, attackedResponse,
                        attackerPlayer, attackedPlayer);
                attacked.setAtk(attacked.getAtk() + attacked.getCardInfo().getAtkIncrease());
                attacked.increaseMaxHp(attacked.getCardInfo().getHpIncrease());

            }
            if (attacked.getHp() > 0 && (attacked.getCardInfo().getId() == CardRepository.CardTemplate.SlimeCommander.getId() ||
                    attacked.getCardInfo().getId() == CardRepository.CardTemplate.HeartStone.getId())) {
                dealHeroDamageOnPunishment(attackerHero, attackedHero, attacked.getCardInfo().getHeroDamage(),
                        attackerResponse, attackedResponse, attackerPlayer, attackedPlayer);
            }
        }
        if (attacker.getCardInfo().getKeyWords().contains(CardRepository.KeyWord.ALIGNMENT)) {
            CardRepository.Status previous = attacked.getCurrentAlignedStatus();
            CardRepository.Status alignment = AlignmentUtil.getAlignment(attacker);
            if (previous != null)
                AlignmentUtil.onAlignedStatusApply(attacker, attacked, attackedField, attackedIndexes, previous, alignment, attackerPlayer, room);
            CardRepository.Status newAlignment = attacked.addStatus(alignment);
            if (newAlignment == null) CardUtil.putCardStatus(attackerResponse, attackedResponse, "no_aligned");
            else CardUtil.putCardStatus(attackerResponse, attackedResponse, newAlignment.toString());
        }
    }
    public static void decreaseHpFromNeighbourCard(int hpDecrease, int pos, List<Card> field, List<Integer> indexes) {
        try {
            Card card = field.get(pos);
            card.decreaseHp(hpDecrease);
            indexes.add(pos);
        } catch (IndexOutOfBoundsException ignored) {}
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
}
