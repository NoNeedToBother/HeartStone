package ru.kpfu.itis.paramonov.heartstone.net.server.room.util;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.PlayerData;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class CardUtil {

    public static final int MAX_FIELD_SIZE = 6;

    public static void removeDefeatedCards(List<Card> field) {
        List<Card> defeatedCards = new ArrayList<>();
        for (Card card : field) {
            if (card.getHp() <= 0) defeatedCards.add(card);
        }
        field.removeAll(defeatedCards);
    }

    public static void changeCardAbilityToAttackOnTurnEnd(PlayerData playerData, GameServer.Client player,
                                                          GameServer.Client otherPlayer, GameRoom room) {
        List<Card> field = playerData.getField();
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
                sendGetChangeResponses(responsePlayer, responseOtherPlayer, player, otherPlayer, room);
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

    public static void checkCardEffectsOnCardDrawn(List<Card> field, GameServer.Client player, GameServer.Client other,
                                                   GameRoom room) {
        for (Card card : field) {
            if (card.getCardInfo().getId() == CardRepository.CardTemplate.ProfessorDog.getId()) {
                card.setAtk(card.getAtk() + card.getCardInfo().getAtkIncrease());
                card.increaseMaxHp(card.getCardInfo().getHpIncrease());
                JSONObject response = new JSONObject();
                JSONObject otherResponse = new JSONObject();
                room.putRoomAction(response, otherResponse, GameRoom.RoomAction.GET_CHANGE);
                response.put("pos", field.indexOf(card));
                putCardStatsAndId(card, response);

                otherResponse.put("opponent_pos", field.indexOf(card));
                putCardStatsAndId(card, otherResponse);

                room.sendResponses(response, otherResponse, player, other);
            }
        }
    }

    public static void sendGetChangeResponses(JSONObject responsePlayer1, JSONObject responsePlayer2, GameServer.Client player1,
                                              GameServer.Client player2, GameRoom room) {
        room.getServer().sendResponse(responsePlayer1.toString(), player1);
        room.getServer().sendResponse(responsePlayer2.toString(), player2);
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
        checkShield(damagedCard, responseDamaged);
        checkShield(damagedCard, responseOther);

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
            checkShield(card, changedCard);

            changes.put(changedCard);
        }
        return changes;
    }

    public static void checkEndTurnCards(GameServer.Client player, GameRoom room) {
        checkForHypnoShroom(player, room);
        for (Card card : room.getPlayerData(player).getField()) {
            if (card.getCardInfo().getId() == CardRepository.CardTemplate.Postman.getId()) {
                room.drawCard(player);
            }
            if (card.hasAction(CardRepository.Action.GIVE_CARD_ON_TURN_END)) {
                List<Integer> cardIds = giveCard(card);
                PlayerData playerData = room.getPlayerData(player);
                if (playerData.getHand().size() + cardIds.size() > GameRoom.HAND_SIZE) {
                    List<Integer> correctedCardIds = new ArrayList<>();
                    for (int i = 0; i < GameRoom.HAND_SIZE - playerData.getHand().size(); i++) {
                        correctedCardIds.add(cardIds.get(i));
                    }
                    addAndSendGottenCardInfo(correctedCardIds, player, room.getOtherPlayer(player), playerData, room);
                }
                else addAndSendGottenCardInfo(cardIds, player, room.getOtherPlayer(player), playerData, room);
            }
        }
    }

    public static List<Integer> giveCard(Card card) {
        List<Integer> cardIds = new ArrayList<>();
        if (card.getCardInfo().getId() == CardRepository.CardTemplate.StudyingApe.getId()) {
            List<CardRepository.CardTemplate> animalCards = filterCards(cardTemplate ->
                    cardTemplate.getFactions().contains(CardRepository.Faction.ANIMAL));
            Random random = new Random();
            int id = animalCards.get(random.nextInt(animalCards.size())).getId();
            cardIds.add(id);
        }
        return cardIds;
    }

    public static List<CardRepository.CardTemplate> filterCards(Predicate<CardRepository.CardTemplate> condition) {
        return Arrays.stream(CardRepository.CardTemplate.values())
                .filter(condition)
                .toList();
    }

    public static void addAndSendGottenCardInfo(List<Integer> ids, GameServer.Client player, GameServer.Client opponent,
                                                PlayerData playerData, GameRoom room) {
        JSONObject response = new JSONObject();
        JSONObject opponentResponse = new JSONObject();
        room.putRoomAction(response, opponentResponse, GameRoom.RoomAction.ADD_CARDS_TO_HAND);
        JSONArray cards = new JSONArray();
        for (Integer id : ids) {
            Card card = new Card(CardRepository.getCardTemplate(id));
            checkGottenCard(card, playerData);
            playerData.getHand().add(card);
            putCardInfo(card, cards);
        }
        response.put("gotten_cards", cards);
        opponentResponse.put("opponent_hand_size", playerData.getHand().size());
        room.sendResponses(response, opponentResponse, player, opponent);
    }

    private static void checkForHypnoShroom(GameServer.Client player, GameRoom room) {
        GameServer.Client otherPlayer = room.getOtherPlayer(player);
        removeCardsWhenHypnoshroom(room.getPlayerData(player), room.getPlayerData(otherPlayer), player, otherPlayer, room);
    }

    private static void removeCardsWhenHypnoshroom(
            PlayerData playerData, PlayerData otherPlayerData, GameServer.Client player,
            GameServer.Client otherPlayer, GameRoom room) {
        List<Card> removed = new ArrayList<>();
        List<Card> added = new ArrayList<>();
        AtomicInteger stolenAmount = new AtomicInteger();
        playerData.getField().stream()
                .filter(card -> card.getCardInfo().getId() == CardRepository.CardTemplate.HypnoShroom.getId())
                .forEach(card -> {
                    JSONObject responseOtherPlayer = new JSONObject();
                    JSONObject responsePlayer = new JSONObject();
                    Card res = stealCard(playerData.getField(), otherPlayerData.getField(),
                            responsePlayer, responseOtherPlayer, stolenAmount.get());
                    if (res != null) {
                        stolenAmount.getAndIncrement();
                        removed.add(res);
                        added.add(res);
                        room.sendResponses(responsePlayer, responseOtherPlayer, player, otherPlayer);
                    }
                });
        otherPlayerData.getField().removeAll(removed);
        playerData.getField().addAll(added);
    }

    private static Card stealCard(List<Card> playerField, List<Card> otherPlayerField,
                                  JSONObject playerResponse, JSONObject otherPlayerResponse, int stolenAmount) {
        playerResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE.toString());
        otherPlayerResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE.toString());
        Random random = new Random();
        int currentOpponentCardAmount = otherPlayerField.size() - stolenAmount;
        if (currentOpponentCardAmount <= 0 || playerField.size() + stolenAmount >= MAX_FIELD_SIZE) return null;
        int randPos = random.nextInt(currentOpponentCardAmount);
        Card stolenCard = otherPlayerField.get(randPos);

        playerResponse.put("gotten_pos", randPos);
        putCardStatsAndId(stolenCard, playerResponse);

        otherPlayerResponse.put("stolen_pos", randPos);
        putCardStatsAndId(stolenCard, otherPlayerResponse);
        return stolenCard;
    }

    public static void decreaseCost(CardRepository.CardTemplate cardToDecrease, PlayerData playerData,
                                    GameServer.Client player, GameRoom room) {
        for (Card card : playerData.getHand()) {
            if (card.getCardInfo().getId() == cardToDecrease.getId()) {
                decreaseCostAndSend(cardToDecrease.getCostDecrease(), player, playerData, card, room);
            }
        }
        for (Card card : playerData.getDeck()) {
            if (card.getCardInfo().getId() == cardToDecrease.getId()) {
                card.setCost(card.getCost() - cardToDecrease.getCostDecrease());
            }
        }
    }

    private static void decreaseCostAndSend(int decrease, GameServer.Client player, PlayerData playerData,
                                            Card card, GameRoom room) {
        card.setCost(card.getCost() - decrease);
        JSONObject responsePlayer = new JSONObject();
        responsePlayer.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer.put("hand_pos", playerData.getHand().indexOf(card));
        responsePlayer.put("cost", card.getCost());
        room.getServer().sendResponse(responsePlayer.toString(), player);
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

    public static void checkShield(Card card, JSONObject response) {
        if (card.hasStatus(CardRepository.Status.SHIELD_REMOVED_2)) {
            card.removeStatus(CardRepository.Status.SHIELD_REMOVED_2);
            response.put("shield_status", "removed");
        }
        else if (card.hasStatus(CardRepository.Status.SHIELD_REMOVED_1)) {
            card.removeStatus(CardRepository.Status.SHIELD_REMOVED_1);
            card.addStatus(CardRepository.Status.SHIELD_REMOVED_2);
            response.put("shield_status", "removed");
        }
        if (card.hasStatus(CardRepository.Status.SHIELD_GIVEN_2)) {
            card.removeStatus(CardRepository.Status.SHIELD_GIVEN_2);
            response.put("shield_status", "given");
        }
        else if (card.hasStatus(CardRepository.Status.SHIELD_GIVEN_1)) {
            card.removeStatus(CardRepository.Status.SHIELD_GIVEN_1);
            card.addStatus(CardRepository.Status.SHIELD_GIVEN_2);
            response.put("shield_status", "given");
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

    private static void checkGottenCard(Card card, PlayerData playerData) {
        if (card.hasKeyWord(CardRepository.KeyWord.GIANT)) {
            List<Card> playedCards = playerData.getPlayedCards();
            switch (card.getCardInfo()) {
                case AnimalKing -> decreaseCostForCondition(card, playedCard -> playedCard.hasFaction(CardRepository.Faction.ANIMAL), playedCards);
                case StoneGiant -> decreaseCostForCondition(card, playedCard -> playedCard.hasFaction(CardRepository.Faction.STONE), playedCards);
                case WaterGiant -> decreaseCostForCondition(card, playedCard -> playedCard.hasFaction(CardRepository.Faction.ELEMENTAL), playedCards);
                case STAN_3000 -> decreaseCostForCondition(card, playedCard -> playedCard.hasFaction(CardRepository.Faction.ROBOT), playedCards);
                case Deity -> decreaseCostForCondition(card, playedCard -> playedCard.hasKeyWord(CardRepository.KeyWord.GIANT), playedCards);
            }
        }
    }

    private static void decreaseCostForCondition(Card card, Predicate<Card> condition, List<Card> playedCards) {
        for (Card playedCard : playedCards) {
            if (condition.test(playedCard)) card.decreaseCost(card.getCardInfo().getCostDecrease());
        }
    }
}
