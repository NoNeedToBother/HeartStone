package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardHelper {
    public static Card onCardPlayed(JSONObject msg, GameServer.Client client, GameServer.Client player1,
                                    HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards) {
        Map<String, List<Card>> allCards = getAllCards(client, player1, player1AllCards, player2AllCards);
        int pos = Integer.parseInt(msg.getString("pos"));
        List<Card> hand = allCards.get("hand");
        Card card = hand.remove(pos);
        List<Card> field = allCards.get("field");
        field.add(card);
        return card;
    }

    private static HashMap<String, List<Card>> getAllCards(GameServer.Client client, GameServer.Client player1,
                                                           HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards) {
        HashMap<String, List<Card>> allCards;
        if (client.equals(player1)) allCards = player1AllCards;
        else allCards = player2AllCards;
        return allCards;
    }

    public static void putPlayedCardForOpponent(JSONObject response, Card playedCard) {
        response.put("room_action", GameRoom.RoomAction.PLAY_CARD_OPPONENT.toString());
        response.put("status", "ok");
        response.put("hp", playedCard.getHp());
        response.put("atk", playedCard.getAtk());
        response.put("cost", playedCard.getCost());
        response.put("id", playedCard.getCardInfo().getId());
    }

    private static final int MAX_FIELD_SIZE = 6;

    public static void checkCardPlayed(JSONObject response, GameServer.Client client, GameServer.Client player1, int handPos,
                                          HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards, int mana) {
        HashMap<String, List<Card>> allCards = getAllCards(client, player1, player1AllCards, player2AllCards);
        if (allCards.get("field").size() == MAX_FIELD_SIZE) {
            response.put("status", "not_ok");
            response.put("reason", "Full field");
            return;
        }
        if (allCards.get("hand").get(handPos).getCost() > mana) {
            response.put("status", "not_ok");
            response.put("reason", "Insufficient mana");
            return;
        }

        response.put("hand_pos", handPos);
        response.put("status", "ok");
    }

    public static void checkCardToAttack(JSONObject response, Card attacker, int pos, String target) {
        response.put("room_action", GameRoom.RoomAction.CHECK_CARD_TO_ATTACK);
        response.put("pos", pos);
        response.put("target", target);
        boolean canAttack = true;
        for (CardRepository.Status status : attacker.getStatuses()) {
            if (status.equals(CardRepository.Status.CANNOT_ATTACK) || status.equals(CardRepository.Status.FROZEN) ||
                    status.equals(CardRepository.Status.ATTACKED)) {
                canAttack = false;
                break;
            }
        }
        if (canAttack) response.put("status", "ok");
        else response.put("status", "not_ok");
    }

    public static void checkCardToAttack(JSONObject response, Card attacker, int pos, int opponentPos, String target) {
        checkCardToAttack(response, attacker, pos, target);
        response.put("opponent_pos", opponentPos);
    }

    public static void decreaseHpOnDirectAttack(Card attacker, Card attacked) {
        attacked.decreaseHp(attacker.getAtk());
        attacker.decreaseHp(attacked.getAtk());
    }

    public static void removeDefeatedCards(List<Card> field) {
        List<Card> defeatedCards = new ArrayList<>();
        for (Card card : field) {
            if (card.getHp() <= 0) defeatedCards.add(card);
        }
        field.removeAll(defeatedCards);
    }

    public static void makeCardsAbleToAttack(List<Card> field) {
        for (Card card : field) {
            card.removeStatus(CardRepository.Status.ATTACKED);
        }
    }

    public static void makeCardsUnableToAttackOnStart(List<Card> deck) {
        for (Card card : deck) {
            CardRepository.CardTemplate cardInfo = card.getCardInfo();
            if (!cardInfo.getActions().contains(CardRepository.CardAction.RUSH_ON_PLAY)) {
                card.addStatus(CardRepository.Status.ATTACKED);
            }
        }
    }

    public static void checkBattleCry(GameServer.Client client, GameServer.Client player1, HashMap<String, List<Card>> player1AllCards,
                                      HashMap<String, List<Card>> player2AllCards, Card playedCard, JSONObject message,
                                      JSONObject responsePlayer1, JSONObject responsePlayer2) {
        responsePlayer1.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer2.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        if (client.equals(player1)) {
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DAMAGE_OPPONENT_ON_PLAY)) {
                checkDamageOnPlay(player2AllCards, message, playedCard, responsePlayer2, responsePlayer1);
            }
        }
        else {
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DAMAGE_OPPONENT_ON_PLAY)) {
                checkDamageOnPlay(player1AllCards, message, playedCard, responsePlayer1, responsePlayer2);
            }
        }
        removeDefeatedCards(player1AllCards.get("field"));
        removeDefeatedCards(player2AllCards.get("field"));
    }

    public static void checkDamageOnPlay(HashMap<String, List<Card>> allCards, JSONObject message, Card playedCard,
                                         JSONObject responseDamaged, JSONObject responseOther) {
        Card damagedCard = allCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.FireElemental.getId()) {
            for (CardRepository.CardAction action : playedCard.getCardInfo().getActions()) {
                if (action.getDamage() != 0) {
                    damagedCard.setHp(damagedCard.getHp() - action.getDamage());
                    putDamagedCardInfo(damagedCard, Integer.parseInt(message.getString("opponent_pos")), responseDamaged, responseOther);
                }
            }
        }
    }

    private static void putDamagedCardInfo(Card damagedCard, int pos, JSONObject responseDamaged, JSONObject responseOther) {
        responseDamaged.put("pos", pos);
        responseOther.put("opponent_pos", pos);
        responseDamaged.put("hp", damagedCard.getHp());
        responseOther.put("hp", damagedCard.getHp());
        responseDamaged.put("atk", damagedCard.getAtk());
        responseOther.put("atk", damagedCard.getAtk());
    }
}
