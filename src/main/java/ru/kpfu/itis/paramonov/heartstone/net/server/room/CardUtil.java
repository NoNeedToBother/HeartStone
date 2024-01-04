package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CardUtil {

    public static HashMap<String, List<Card>> getAllCards(GameServer.Client client, GameServer.Client player1,
                                                           HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards) {
        HashMap<String, List<Card>> allCards;
        if (client.equals(player1)) allCards = player1AllCards;
        else allCards = player2AllCards;
        return allCards;
    }

    private static final int MAX_FIELD_SIZE = 6;

    public static int getMaxFieldSize() {
        return MAX_FIELD_SIZE;
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
            if (card.hasStatus(CardRepository.Status.FROZEN_2)) {
                card.removeStatus(CardRepository.Status.FROZEN_2);
                card.addStatus(CardRepository.Status.FROZEN_1);
            }
            else if (card.hasStatus(CardRepository.Status.FROZEN_1)) {
                card.removeStatus(CardRepository.Status.FROZEN_1);
                card.removeStatus(CardRepository.Status.FROZEN);
                JSONObject responsePlayer = new JSONObject();
                JSONObject responseOtherPlayer = new JSONObject();
                responsePlayer.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                responseOtherPlayer.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                putFrozenCardInfo(field.indexOf(card), responsePlayer, responseOtherPlayer, true);
                sendGetChangeResponses(responsePlayer, responseOtherPlayer, player, otherPlayer, server);
            }
            card.removeStatus(CardRepository.Status.ATTACKED);
        }
    }

    public static void makeCardsUnableToAttackOnStart(List<Card> deck) {
        for (Card card : deck) {
            if (!(card.hasAction(CardRepository.Action.RUSH_ON_PLAY) || card.hasAction(CardRepository.Action.BOARD_ON_PLAY)))
                card.addStatus(CardRepository.Status.ATTACKED);
            if (card.hasAction(CardRepository.Action.BOARD_ON_PLAY)) {
                card.addStatus(CardRepository.Status.CAN_ATTACK_CARDS_ON_PLAY);
            }
        }
    }

    public static void checkProfessorDogOnCardDraw(List<Card> field, GameServer.Client player, GameServer.Client other, GameServer server) {
        for (Card card : field) {
            if (card.getCardInfo().getId() == CardRepository.CardTemplate.ProfessorDog.getId()) {
                card.setAtk(card.getAtk() + card.getCardInfo().getAtkIncrease());
                card.increaseMaxHp(card.getCardInfo().getHpIncrease());
                JSONObject response = new JSONObject();
                JSONObject otherResponse = new JSONObject();
                response.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                otherResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                response.put("pos", field.indexOf(card));
                putCardStatsAndId(card, response);

                otherResponse.put("opponent_pos", field.indexOf(card));
                putCardStatsAndId(card, otherResponse);

                server.sendResponse(response.toString(), player);
                server.sendResponse(otherResponse.toString(), other);
            }
        }
    }

    public static void sendGetChangeResponses(JSONObject responsePlayer1, JSONObject responsePlayer2, GameServer.Client player1,
                                              GameServer.Client player2, GameServer server) {
        server.sendResponse(responsePlayer1.toString(), player1);
        server.sendResponse(responsePlayer2.toString(), player2);
        clearGetChangeResponses(responsePlayer1, responsePlayer2);
    }

    private static void clearGetChangeResponses(JSONObject responsePlayer1, JSONObject responsePlayer2) {
        responsePlayer1 = new JSONObject();
        responsePlayer2 = new JSONObject();
        responsePlayer1.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer2.put("room_action", GameRoom.RoomAction.GET_CHANGE);
    }

    public static int getDefeatedCardAmount(List<Card> field) {
        int res = 0;
        for (Card card : field) {
            if (card.getHp() <= 0) res += 1;
        }
        return res;
    }

    public static void putDamagedCardInfo(Card damagedCard, int pos, JSONObject responseDamaged, JSONObject responseOther) {
        responseDamaged.put("pos", pos);
        responseOther.put("opponent_pos", pos);
        responseDamaged.put("hp", damagedCard.getHp());
        responseOther.put("hp", damagedCard.getHp());
        responseDamaged.put("atk", damagedCard.getAtk());
        responseOther.put("atk", damagedCard.getAtk());
        checkShieldRemoved(damagedCard, responseDamaged);
        checkShieldRemoved(damagedCard, responseOther);

    }

    public static void putFrozenCardInfo(int pos, JSONObject responseDamaged, JSONObject responseOther, boolean unfrozen) {
        responseDamaged.put("pos", pos);
        responseOther.put("opponent_pos", pos);
        if (unfrozen) putCardStatus(responseDamaged, responseOther, "no_frozen");
        else putCardStatus(responseDamaged, responseOther, CardRepository.Status.FROZEN.toString());
    }

    public static void putCardStatus(JSONObject responsePlayer1, JSONObject responsePlayer2, String cardStatus) {
        responsePlayer1.put("card_status", cardStatus);
        responsePlayer2.put("card_status", cardStatus);
    }

    public static void putFieldChanges(JSONObject response, List<Card> field, List<Card> opponentField,
                                       List<Integer> positions, List<Integer> opponentPositions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("stat_changes", changes);
        JSONArray opponentChanges = getFieldChanges(opponentField, opponentPositions);
        response.put("opponent_stat_changes", opponentChanges);
    }

    public static JSONArray getFieldChanges(List<Card> field, List<Integer> positions) {
        JSONArray changes = new JSONArray();
        for (Integer position : positions) {
            JSONObject changedCard = new JSONObject();
            Card card = field.get(position);
            changedCard.put("pos", position);
            changedCard.put("hp", card.getHp());
            changedCard.put("atk", card.getAtk());
            if (card.getCurrentAlignedStatus() != null) changedCard.put("aligned_status", card.getCurrentAlignedStatus().toString());
            else changedCard.put("aligned_status", "no_aligned");
            checkShieldRemoved(card, changedCard);

            changes.put(changedCard);
        }
        return changes;
    }

    public static void checkEndTurnCards(GameServer.Client client, GameServer.Client player1, GameServer.Client player2, HashMap<String, List<Card>> player1AllCards,
                                         HashMap<String, List<Card>> player2AllCards, GameServer server, GameRoom room) {
        checkForHypnoShroom(client, player1, player2, player1AllCards, player2AllCards, server);
        if (client.equals(player1)) {
            for (Card card : player1AllCards.get("field")) {
                if (card.getCardInfo().getId() == CardRepository.CardTemplate.Postman.getId()) {
                    room.drawCard(player1);
                }
            }
        } else {
            for (Card card : player2AllCards.get("field")) {
                if (card.getCardInfo().getId() == CardRepository.CardTemplate.Postman.getId()) {
                    room.drawCard(player2);
                }
            }
        }
    }

    private static void checkForHypnoShroom(GameServer.Client client, GameServer.Client player1, GameServer.Client player2, HashMap<String, List<Card>> player1AllCards,
                                         HashMap<String, List<Card>> player2AllCards, GameServer server) {
        if (client.equals(player1)) {
            removeCardsWhenHypnoshroom(player1AllCards, player2AllCards, player1, player2, server);
        }
        else {
            removeCardsWhenHypnoshroom(player2AllCards, player1AllCards, player2, player1, server);
        }
    }

    private static void removeCardsWhenHypnoshroom(
            HashMap<String, List<Card>> playerAllCards, HashMap<String, List<Card>> otherPlayerAllCards, GameServer.Client player,
            GameServer.Client otherPlayer, GameServer server) {
        List<Card> removed = new ArrayList<>();
        List<Card> added = new ArrayList<>();
        AtomicInteger stolenAmount = new AtomicInteger();
        playerAllCards.get("field").stream()
                .filter(card -> card.getCardInfo().getId() == CardRepository.CardTemplate.HypnoShroom.getId())
                .forEach(card -> {
                    JSONObject responseOtherPlayer = new JSONObject();
                    JSONObject responsePlayer = new JSONObject();
                    Card res = checkHypnoshroom(playerAllCards, otherPlayerAllCards, responsePlayer, responseOtherPlayer, stolenAmount.get());
                    if (res != null) {
                        stolenAmount.getAndIncrement();
                        removed.add(res);
                        added.add(res);
                        server.sendResponse(responseOtherPlayer.toString(), otherPlayer);
                        server.sendResponse(responsePlayer.toString(), player);
                    }
                });
        otherPlayerAllCards.get("field").removeAll(removed);
        playerAllCards.get("field").addAll(added);
    }

    private static Card checkHypnoshroom(HashMap<String, List<Card>> playerCards, HashMap<String, List<Card>> otherPlayerCards,
                                                     JSONObject playerResponse, JSONObject otherPlayerResponse, int stolenAmount) {
        playerResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE.toString());
        otherPlayerResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE.toString());
        Random random = new Random();
        int currentOpponentCardAmount = otherPlayerCards.get("field").size() - stolenAmount;
        if (currentOpponentCardAmount <= 0 || playerCards.get("field").size() + stolenAmount >= MAX_FIELD_SIZE) return null;
        int randPos = random.nextInt(currentOpponentCardAmount);
        Card stolenCard = otherPlayerCards.get("field").get(randPos);

        playerResponse.put("gotten_pos", randPos);
        putCardStatsAndId(stolenCard, playerResponse);

        otherPlayerResponse.put("stolen_pos", randPos);
        putCardStatsAndId(stolenCard, otherPlayerResponse);
        return stolenCard;
    }

    public static void decreaseCost(CardRepository.CardTemplate cardToDecrease, HashMap<String, List<Card>> allCards,
                                    GameServer.Client player, GameServer server) {
        for (Card card : allCards.get("hand")) {
            if (card.getCardInfo().getId() == cardToDecrease.getId()) {
                decreaseCostAndSend(cardToDecrease.getCostDecrease(), player, allCards, server, card);
            }
        }
        for (Card card : allCards.get("deck")) {
            if (card.getCardInfo().getId() == cardToDecrease.getId()) {
                card.setCost(card.getCost() - cardToDecrease.getCostDecrease());
            }
        }
    }

    private static void decreaseCostAndSend(int decrease, GameServer.Client player, HashMap<String, List<Card>> allCards, GameServer server, Card card) {
        card.setCost(card.getCost() - decrease);
        JSONObject responsePlayer = new JSONObject();
        responsePlayer.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer.put("hand_pos", allCards.get("hand").indexOf(card));
        responsePlayer.put("cost", card.getCost());
        server.sendResponse(responsePlayer.toString(), player);
    }

    public static void putCardInfo(Card card, JSONArray responseCards) {
        JSONObject jsonCard = getJsonCard(card);
        responseCards.put(jsonCard);
    }

    public static void putCardInfo(Card card, JSONObject response) {
        JSONObject jsonCard = getJsonCard(card);
        response.put("card", jsonCard);
    }

    public static void putCardStatsAndId(Card card, JSONObject response) {
        response.put("hp", card.getHp());
        response.put("atk", card.getAtk());
        response.put("cost", card.getCost());
        response.put("id", card.getCardInfo().getId());
    }

    public static void putStatuses(List<CardRepository.Status> statuses, JSONObject response) {
        JSONArray responseStatuses = new JSONArray();
        for (CardRepository.Status status : statuses) {
            responseStatuses.put(status.toString());
        }
        response.put("statuses", responseStatuses);
    }

    public static void checkShieldRemoved(Card card, JSONObject response) {
        if (card.hasStatus(CardRepository.Status.SHIELD_REMOVED_2)) {
            card.removeStatus(CardRepository.Status.SHIELD_REMOVED_2);
            response.put("shield_status", "removed");
        }
        if (card.hasStatus(CardRepository.Status.SHIELD_REMOVED_1)) {
            card.removeStatus(CardRepository.Status.SHIELD_REMOVED_1);
            card.addStatus(CardRepository.Status.SHIELD_REMOVED_2);
            response.put("shield_status", "removed");
        }
    }

    private static JSONObject getJsonCard(Card card) {
        JSONObject jsonCard = new JSONObject();
        jsonCard.put("atk", card.getAtk());
        jsonCard.put("hp", card.getHp());
        jsonCard.put("cost", card.getCost());
        CardRepository.CardTemplate cardInfo = card.getCardInfo();
        jsonCard.put("id", cardInfo.getId());
        return jsonCard;
    }
}
