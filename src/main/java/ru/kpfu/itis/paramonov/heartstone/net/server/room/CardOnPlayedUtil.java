package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardOnPlayedUtil {
    public static Card addCardOnFieldWhenCardPlayed(JSONObject msg, GameServer.Client client, GameRoom room) {
        Map<String, List<Card>> allCards = room.getAllCards(client);
        int pos = msg.getInt("pos");
        List<Card> hand = allCards.get("hand");
        Card card = hand.remove(pos);
        List<Card> field = allCards.get("field");
        field.add(card);
        return card;
    }

    public static void checkCardPlayed(JSONObject response, GameServer.Client client, int handPos, int mana, GameRoom room) {
        HashMap<String, List<Card>> allCards = room.getAllCards(client);
        if (!client.equals(room.getActivePlayer())) {
            response.put("status", "not_ok");
            response.put("reason", "Not your turn");
        }
        else if (allCards.get("field").size() == CardUtil.MAX_FIELD_SIZE) {
            response.put("status", "not_ok");
            response.put("reason", "Full field");
        }
        else if (allCards.get("hand").get(handPos).getCost() > mana) {
            response.put("status", "not_ok");
            response.put("reason", "Insufficient mana");
        }
        else {
            response.put("hand_pos", handPos);
            response.put("status", "ok");
        }
    }
    public static void checkBattleCry(GameServer.Client player, Card playedCard, JSONObject message,
                                      JSONObject responsePlayer, JSONObject responseTargeted, GameRoom room) {
        room.putRoomAction(responsePlayer, responseTargeted, GameRoom.RoomAction.GET_CHANGE);
        GameServer.Client otherPlayer = room.getOtherPlayer(player);
        HashMap<String, List<Card>> playerAllCards = room.getAllCards(player);
        HashMap<String, List<Card>> otherPlayerAllCards = room.getAllCards(otherPlayer);
        checkTargetedCardsOnBattleCry(player, playedCard, message, responsePlayer, responseTargeted, room);
        if (playedCard.hasAction(CardRepository.Action.DRAW_CARD_ON_PLAY)) {
            for (int i = 0; i < playedCard.getCardInfo().getDrawnCards(); i++) {
                room.drawCard(player);
                if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Illusionist.getId()) {
                    room.drawCard(otherPlayer);
                }
            }
        }
        if (playedCard.hasAction(CardRepository.Action.DAMAGE_ALL_ON_PLAY)) {
            checkAllCardsDamageOnPlay(playerAllCards, otherPlayerAllCards, playedCard, responsePlayer, responseTargeted);
            CardUtil.sendGetChangeResponses(responsePlayer, responseTargeted, player, otherPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.GIVE_SHIELD_ON_PLAY)) {
            List<Card> field = room.getAllCards(player).get("field");
            List<Integer> playerIndexes = new ArrayList<>();
            checkGivenShield(field.indexOf(playedCard), playedCard, field, playerIndexes);
            CardUtil.putFieldChanges(responsePlayer, playerAllCards.get("field"), otherPlayerAllCards.get("field"), playerIndexes, List.of());
            CardUtil.putFieldChanges(responseTargeted, otherPlayerAllCards.get("field"), playerAllCards.get("field"), List.of(), playerIndexes);
            CardUtil.sendGetChangeResponses(responsePlayer, responseTargeted, player, room.getOtherPlayer(player), room);
        }
        CardUtil.removeDefeatedCards(room.getAllCards(player).get("field"));
        CardUtil.removeDefeatedCards(room.getAllCards(otherPlayer).get("field"));
    }

    private static void checkGivenShield(int pos, Card playedCard, List<Card> field, List<Integer> indexes) {
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Paladin.getId()) {
            giveShield(pos + 1, field, indexes);
            giveShield(pos - 1, field, indexes);
        }
    }

    private static void giveShield(int pos, List<Card> field, List<Integer> indexes) {
        try {
            Card card = field.get(pos);
            card.addStatus(CardRepository.Status.SHIELDED);
            card.addStatus(CardRepository.Status.SHIELD_GIVEN_1);
            indexes.add(pos);
        } catch (IndexOutOfBoundsException ignored) {}
    }

    public static void checkTargetedCardsOnBattleCry(GameServer.Client player, Card playedCard,
                                                     JSONObject message, JSONObject response, JSONObject responseTargeted,
                                                     GameRoom room) {
        GameServer.Client targetedPlayer = room.getOtherPlayer(player);
        HashMap<String, List<Card>> otherPlayerAllCards = room.getAllCards(targetedPlayer);
        if (playedCard.hasAction(CardRepository.Action.FREEZE_ENEMY_ON_PLAY)) {
            freezeEnemyOnPlay(otherPlayerAllCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.DAMAGE_ENEMY_ON_PLAY)) {
            checkEnemyDamageOnPlay(player, message.getInt("opponent_pos"), playedCard, responseTargeted, response, room);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.DESTROY_ENEMY_ON_PLAY)) {
            checkDestroyOnPlay(otherPlayerAllCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.FREEZE_ADJACENT_CARDS_ON_PLAY)) {
            freezeEnemyAndAdjacentOnesOnPlay(otherPlayerAllCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
    }

    public static void freezeEnemyOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard,
                                         JSONObject responseTargeted, JSONObject responseOther) {
        Card frozenCard = allTargetedCards.get("field").get(message.getInt("opponent_pos"));
        frozenCard.addStatus(CardRepository.Status.FROZEN_1);
        CardUtil.putFrozenCardInfo(message.getInt("opponent_pos"), responseTargeted, responseOther, false);
    }

    public static void freezeEnemyAndAdjacentOnesOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard,
                                                        JSONObject responseTargeted, JSONObject responseOther) {
        int frozenPos = message.getInt("opponent_pos");
        List<Integer> indexes = new ArrayList<>();
        Card frozenCard = allTargetedCards.get("field").get(frozenPos);
        frozenCard.addStatus(CardRepository.Status.FROZEN_1);
        freezeAdjacentCards(allTargetedCards.get("field"), frozenPos, indexes);
        JSONArray frozenPositions = new JSONArray();
        frozenPositions.put(frozenPos);
        for (Integer pos : indexes) {
            frozenPositions.put(pos);
        }
        responseOther.put("opponent_frozen_positions", frozenPositions);
        responseTargeted.put("frozen_positions", frozenPositions);
    }

    private static void freezeAdjacentCards(List<Card> field, int targetedPos, List<Integer> indexes) {
        List<Integer> positionsToFreeze = List.of(targetedPos - 1, targetedPos + 1);
        for (Integer positionToFreeze : positionsToFreeze) {
            try {
                Card frozenCard = field.get(positionToFreeze);
                frozenCard.addStatus(CardRepository.Status.FROZEN_1);
                indexes.add(positionToFreeze);
            } catch (IndexOutOfBoundsException ignored) {}
        }
    }

    public static void checkEnemyDamageOnPlay(GameServer.Client attackerPlayer, int opponentPos, Card playedCard,
                                              JSONObject responseTargeted, JSONObject response, GameRoom room) {
        List<Card> playerField = room.getAllCards(attackerPlayer).get("field");
        List<Card> targetedField = room.getAllCards(room.getOtherPlayer(attackerPlayer)).get("field");
        Card damagedCard = targetedField.get(opponentPos);
        if (playedCard.hasAction(CardRepository.Action.DAMAGE_ENEMY_ON_PLAY)) {
            damagedCard.decreaseHp(playedCard.getCardInfo().getDamage());
            if (playedCard.hasAction(CardRepository.Action.DEAL_ENERGY_DMG)) {
                CardRepository.Status previous = damagedCard.getCurrentAlignedStatus();
                List<Integer> attackedIndexes = new ArrayList<>(List.of(opponentPos));
                if (previous != null) {
                    AlignmentUtil.onAlignedStatusApply(playedCard, damagedCard, targetedField, attackedIndexes,
                            previous, CardRepository.Status.ENERGY, attackerPlayer, room);
                }
                CardUtil.putFieldChanges(response, playerField, targetedField, List.of(playerField.indexOf(playedCard)), attackedIndexes);
                CardUtil.putFieldChanges(responseTargeted, targetedField, playerField, attackedIndexes, List.of(playerField.indexOf(playedCard)));
            }
            else CardUtil.putDamagedCardInfo(damagedCard, opponentPos, responseTargeted, response);
        }
    }

    public static void checkAllCardsDamageOnPlay(HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards,
                                                 Card playedCard, JSONObject player1Response, JSONObject player2Response) {
        List<Integer> player1Indexes = new ArrayList<>();
        List<Integer> player2Indexes = new ArrayList<>();
        boolean isCardTrantos = playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Trantos.getId();
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.CrazyPyromaniac.getId() || isCardTrantos) {
            damageAllCardsExceptPlayedAndAdd(player1AllCards.get("field"), playedCard, player1Indexes, isCardTrantos);
            damageAllCardsExceptPlayedAndAdd(player2AllCards.get("field"), playedCard, player2Indexes, isCardTrantos);
            player1Response.put("anim", "field_fire");
            player2Response.put("anim", "field_fire");
            if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Trantos.getId()) {
                int defeatedAmount = CardUtil.getDefeatedCardAmount(player1AllCards.get("field")) +
                        CardUtil.getDefeatedCardAmount(player2AllCards.get("field"));
                playedCard.setAtk(playedCard.getAtk() + playedCard.getCardInfo().getAtkIncrease() * defeatedAmount);
                playedCard.increaseMaxHp(playedCard.getCardInfo().getHpIncrease() * defeatedAmount);
                playedCard.increaseHp(playedCard.getCardInfo().getHpIncrease() * defeatedAmount);
            }
        }
        CardUtil.putFieldChanges(player1Response, player1AllCards.get("field"), player2AllCards.get("field"),
                player1Indexes, player2Indexes);
        CardUtil.putFieldChanges(player2Response, player2AllCards.get("field"), player1AllCards.get("field"),
                player2Indexes, player1Indexes);
    }

    private static void damageAllCardsExceptPlayedAndAdd(List<Card> field, Card playedCard, List<Integer> playerIndexes, boolean addCard) {
        for (Card card : field) {
            if (!card.equals(playedCard)) {
                card.decreaseHp(playedCard.getCardInfo().getAllCardsDamage());
                playerIndexes.add(field.indexOf(card));
            } else {
                if (addCard) playerIndexes.add(field.indexOf(card));
            }
        }
    }

    public static void checkDestroyOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard,
                                          JSONObject responseDestroyed, JSONObject responseOther) {
        Card destroyedCard = allTargetedCards.get("field").get(message.getInt("opponent_pos"));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.StoneAssassin.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Hydra.getId()) {
            destroyedCard.setHp(0);
            CardUtil.putDamagedCardInfo(destroyedCard, message.getInt("opponent_pos"), responseDestroyed, responseOther);
        }
    }

    public static void checkOnCardPlayed(GameServer.Client player, HashMap<String, List<Card>> allCards, Card playedCard,
                                         GameRoom room) {
        List<CardRepository.Faction> factions = playedCard.getCardInfo().getFactions();
        for (CardRepository.Faction faction : factions) {
            switch (faction) {
                case STONE -> CardUtil.decreaseCost(CardRepository.CardTemplate.StoneGiant, allCards, player, room);
                case ELEMENTAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.WaterGiant, allCards, player, room);
                case ANIMAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.AnimalKing, allCards, player, room);
                case ROBOT -> CardUtil.decreaseCost(CardRepository.CardTemplate.STAN_3000, allCards, player, room);
            }
        }
        if (playedCard.hasKeyWord(CardRepository.KeyWord.GIANT)) {
            CardUtil.decreaseCost(CardRepository.CardTemplate.Deity, allCards, player, room);
        }
    }
}
