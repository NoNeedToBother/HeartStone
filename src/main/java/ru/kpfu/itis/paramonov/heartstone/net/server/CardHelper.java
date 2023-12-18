package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONArray;
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
            if (CardRepository.Status.getCardAttackRestrictionStatuses().contains(status)) {
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

    public static void changeCardAbilityToAttackOnTurnEnd(List<Card> field, GameServer.Client player, GameServer.Client otherPlayer,
                                                          GameServer server) {
        for (Card card : field) {
            if (card.getStatuses().contains(CardRepository.Status.FROZEN_2)) {
                card.removeStatus(CardRepository.Status.FROZEN_2);
                card.addStatus(CardRepository.Status.FROZEN_1);
            }
            else if (card.getStatuses().contains(CardRepository.Status.FROZEN_1)) {
                card.removeStatus(CardRepository.Status.FROZEN_1);
                card.removeStatus(CardRepository.Status.FROZEN);
                JSONObject responsePlayer = new JSONObject();
                JSONObject responseOtherPlayer = new JSONObject();
                responsePlayer.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                responseOtherPlayer.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                putFrozenCardInfo(field.indexOf(card), responsePlayer, responseOtherPlayer, true);
                server.sendResponse(responsePlayer.toString(), player);
                server.sendResponse(responseOtherPlayer.toString(), otherPlayer);
            }
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

    public static void checkBattleCry(GameServer.Client client, GameServer.Client player1, GameServer.Client player2, HashMap<String, List<Card>> player1AllCards,
                                      HashMap<String, List<Card>> player2AllCards, Card playedCard, JSONObject message,
                                      JSONObject responsePlayer1, JSONObject responsePlayer2, GameServer server) {
        responsePlayer1.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer2.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        if (client.equals(player1)) {
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DAMAGE_ENEMY_ON_PLAY)) {
                checkEnemyDamageOnPlay(player2AllCards, message, playedCard, responsePlayer2, responsePlayer1);
                sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
            }
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DESTROY_ENEMY_ON_PLAY)) {
                checkDestroyOnPlay(player2AllCards, message, playedCard, responsePlayer2, responsePlayer1);
                sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
            }
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.FREEZE_ENEMY_ON_PLAY)) {
                freezeEnemyOnPlay(player2AllCards, message, playedCard, responsePlayer2, responsePlayer1);
                sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
            }
        }
        else {
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DAMAGE_ENEMY_ON_PLAY)) {
                checkEnemyDamageOnPlay(player1AllCards, message, playedCard, responsePlayer1, responsePlayer2);
                sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
            }
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DESTROY_ENEMY_ON_PLAY)) {
                checkDestroyOnPlay(player1AllCards, message, playedCard, responsePlayer1, responsePlayer2);
                sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
            }
            if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.FREEZE_ENEMY_ON_PLAY)) {
                freezeEnemyOnPlay(player1AllCards, message, playedCard, responsePlayer2, responsePlayer1);
                sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
            }
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DAMAGE_ON_PLAY)) {
            checkDamageOnPlay(player1AllCards, player2AllCards, playedCard, responsePlayer1, responsePlayer2, client, player1);
            sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
        }
        removeDefeatedCards(player1AllCards.get("field"));
        removeDefeatedCards(player2AllCards.get("field"));
    }

    public static void sendResponses(JSONObject responsePlayer1, JSONObject responsePlayer2, GameServer.Client player1,
                                     GameServer.Client player2, GameServer server) {
        server.sendResponse(responsePlayer1.toString(), player1);
        server.sendResponse(responsePlayer2.toString(), player2);
        clearResponses(responsePlayer1, responsePlayer2);
    }

    private static void clearResponses(JSONObject responsePlayer1, JSONObject responsePlayer2) {
        responsePlayer1 = new JSONObject();
        responsePlayer2 = new JSONObject();
        responsePlayer1.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer2.put("room_action", GameRoom.RoomAction.GET_CHANGE);
    }

    public static void freezeEnemyOnPlay(HashMap<String, List<Card>> allCards, JSONObject message, Card playedCard, JSONObject responseTargeted, JSONObject responseOther) {
        Card frozenCard = allCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.IceElemental.getId()) {
            frozenCard.addStatus(CardRepository.Status.FROZEN_1);
            putFrozenCardInfo(Integer.parseInt(message.getString("opponent_pos")), responseTargeted, responseOther, false);
        }
    }

    public static void checkEnemyDamageOnPlay(HashMap<String, List<Card>> allCards, JSONObject message, Card playedCard,
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

    public static void checkDamageOnPlay(HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards,
                                         Card playedCard, JSONObject player1Response, JSONObject player2Response, GameServer.Client client, GameServer.Client player1) {
        List<Integer> player1Indexes = new ArrayList<>();
        List<Integer> player2Indexes = new ArrayList<>();
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.CrazyPyromaniac.getId()) {
            damageAllCardsExceptPlayedAndAdd(player1AllCards.get("field"), playedCard, player1Indexes, false);
            damageAllCardsExceptPlayedAndAdd(player2AllCards.get("field"), playedCard, player2Indexes, false);
        }
        else if(playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Trantos.getId()) {
            damageAllCardsExceptPlayedAndAdd(player1AllCards.get("field"), playedCard, player1Indexes, true);
            damageAllCardsExceptPlayedAndAdd(player2AllCards.get("field"), playedCard, player2Indexes, true);
            int defeatedAmount = getDefeatedCardAmount(player1AllCards.get("field")) + getDefeatedCardAmount(player2AllCards.get("field"));
            if (defeatedAmount != 0) {
                for (CardRepository.CardAction action : playedCard.getCardInfo().getActions()) {
                    if (action.toString().equals(CardRepository.CardAction.ATK_UP.toString())) {
                        playedCard.setAtk(playedCard.getAtk() + action.getAtkIncrease() * defeatedAmount);
                    }
                    if (action.toString().equals(CardRepository.CardAction.HP_UP.toString())) {
                        playedCard.setHp(playedCard.getHp() + action.getHpIncrease() * defeatedAmount);
                    }
                }
            }
        }
        putFieldChanges(player1Response, player1AllCards.get("field"), player1Indexes);
        putOpponentChanges(player1Response, player2AllCards.get("field"), player2Indexes);

        putOpponentChanges(player2Response, player1AllCards.get("field"), player1Indexes);
        putFieldChanges(player2Response, player2AllCards.get("field"), player2Indexes);
    }

    private static int getDefeatedCardAmount(List<Card> field) {
        int res = 0;
        for (Card card : field) {
            if (card.getHp() <= 0) res += 1;
        }
        return res;
    }

    private static void damageAllCardsExceptPlayedAndAdd(List<Card> field, Card playedCard, List<Integer> playerIndexes, boolean addCard) {
        for (Card card : field) {
            if (!card.equals(playedCard)) {
                for (CardRepository.CardAction action : playedCard.getCardInfo().getActions()) {
                    if (action.toString().equals(CardRepository.CardAction.DAMAGE_ON_PLAY.toString())) {
                        card.setHp(card.getHp() - action.getDamage());
                        playerIndexes.add(field.indexOf(card));
                    }
                }
            } else {
                if (addCard) playerIndexes.add(field.indexOf(card));
            }
        }
    }

    public static void checkDestroyOnPlay(HashMap<String, List<Card>> allCards, JSONObject message, Card playedCard,
                                          JSONObject responseDestroyed, JSONObject responseOther) {
        Card destroyedCard = allCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.StoneAssassin.getId()) {
            destroyedCard.setHp(0);
            putDamagedCardInfo(destroyedCard, Integer.parseInt(message.getString("opponent_pos")), responseDestroyed, responseOther);
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

    private static void putFrozenCardInfo(int pos, JSONObject responseDamaged, JSONObject responseOther, boolean unfrozen) {
        responseDamaged.put("pos", pos);
        responseOther.put("opponent_pos", pos);
        if (unfrozen) {
            responseDamaged.put("status", "no_frozen");
            responseOther.put("status", "no_frozen");
        }
        else {
            responseDamaged.put("status", CardRepository.Status.FROZEN.toString());
            responseOther.put("status", CardRepository.Status.FROZEN.toString());
        }
    }

    public static void putFieldChanges(JSONObject response, List<Card> field, List<Integer> positions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("stat_changes", changes);
    }

    public static JSONArray getFieldChanges(List<Card> field, List<Integer> positions) {
        JSONArray changes = new JSONArray();
        for (int pos = 0; pos < positions.size(); pos++) {
            JSONObject changedCard = new JSONObject();
            changedCard.put("pos", positions.get(pos));
            changedCard.put("hp", field.get(positions.get(pos)).getHp());
            changedCard.put("atk", field.get(positions.get(pos)).getAtk());
            changes.put(changedCard);
        }
        return changes;
    }

    public static void putOpponentChanges(JSONObject response, List<Card> field, List<Integer> positions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("opponent_stat_changes", changes);
    }
}
