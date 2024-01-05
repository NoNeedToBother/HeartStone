package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.controller.PacksController;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardOnPlayedUtil {
    public static Card onCardPlayed(JSONObject msg, GameServer.Client client, GameServer.Client player1,
                                    HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards) {
        Map<String, List<Card>> allCards = CardUtil.getAllCards(client, player1, player1AllCards, player2AllCards);
        int pos = msg.getInt("pos");
        List<Card> hand = allCards.get("hand");
        Card card = hand.remove(pos);
        List<Card> field = allCards.get("field");
        field.add(card);
        return card;
    }

    public static void checkCardPlayed(JSONObject response, GameServer.Client client, GameServer.Client activePlayer, GameServer.Client player1, int handPos,
                                       HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards, int mana) {
        HashMap<String, List<Card>> allCards = CardUtil.getAllCards(client, player1, player1AllCards, player2AllCards);
        if (!client.equals(activePlayer)) {
            response.put("status", "not_ok");
            response.put("reason", "Not your turn");
            return;
        }
        if (allCards.get("field").size() == CardUtil.MAX_FIELD_SIZE) {
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
    public static void checkBattleCry(GameServer.Client client, GameServer.Client player1, GameServer.Client player2, HashMap<String, List<Card>> player1AllCards,
                                      HashMap<String, List<Card>> player2AllCards, Card playedCard, JSONObject message,
                                      JSONObject responsePlayer1, JSONObject responsePlayer2, GameServer server, GameRoom room) {
        responsePlayer1.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer2.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        if (client.equals(player1))
            checkTargetedCardsOnBattleCry(player1, player2, player1AllCards, player2AllCards, playedCard, message, responsePlayer1, responsePlayer2, server, room);
        else
            checkTargetedCardsOnBattleCry(player2, player1, player2AllCards, player1AllCards, playedCard, message, responsePlayer2, responsePlayer1, server, room);
        if (playedCard.hasAction(CardRepository.Action.DRAW_CARD_ON_PLAY)) {
            for (int i = 0; i < playedCard.getCardInfo().getDrawnCards(); i++) {
                room.drawCard(client);
                if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Illusionist.getId()) {
                    room.drawCard(room.getOtherPlayer(client));
                }
            }
        }
        if (playedCard.hasAction(CardRepository.Action.DAMAGE_ALL_ON_PLAY)) {
            checkDamageOnPlay(player1AllCards, player2AllCards, playedCard, responsePlayer1, responsePlayer2, client, player1);
            CardUtil.sendGetChangeResponses(responsePlayer1, responsePlayer2, player1, player2, server);
        }
        if (playedCard.hasAction(CardRepository.Action.GIVE_SHIELD_ON_PLAY)) {
            List<Card> field = room.getAllCards(client).get("field");
            List<Integer> playerIndexes = new ArrayList<>();
            checkGivenShield(field.indexOf(playedCard), playedCard, field, playerIndexes);
            if (client.equals(player1)) {
                CardUtil.putFieldChanges(responsePlayer1, player1AllCards.get("field"), player2AllCards.get("field"), playerIndexes, List.of());
                CardUtil.putFieldChanges(responsePlayer2, player2AllCards.get("field"), player1AllCards.get("field"), List.of(), playerIndexes);
            }
            else {
                CardUtil.putFieldChanges(responsePlayer2, player2AllCards.get("field"), player1AllCards.get("field"), playerIndexes, List.of());
                CardUtil.putFieldChanges(responsePlayer1, player1AllCards.get("field"), player2AllCards.get("field"), List.of(), playerIndexes);
            }
            CardUtil.sendGetChangeResponses(responsePlayer1, responsePlayer2, player1, player2, server);
        }
        CardUtil.removeDefeatedCards(player1AllCards.get("field"));
        CardUtil.removeDefeatedCards(player2AllCards.get("field"));
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

    public static void checkTargetedCardsOnBattleCry(GameServer.Client player, GameServer.Client targetedPlayer, HashMap<String, List<Card>> allPlayerCards,
                                                     HashMap<String, List<Card>> allTargetedCards, Card playedCard,
                                                     JSONObject message, JSONObject response, JSONObject responseTargeted,
                                                     GameServer server, GameRoom room) {
        if (playedCard.hasAction(CardRepository.Action.FREEZE_ENEMY_ON_PLAY)) {
            freezeEnemyOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, server);
        }
        if (playedCard.hasAction(CardRepository.Action.DAMAGE_ENEMY_ON_PLAY)) {
            checkEnemyDamageOnPlay(allPlayerCards, allTargetedCards, message, playedCard, responseTargeted, response, room, player);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, server);
        }
        if (playedCard.hasAction(CardRepository.Action.DESTROY_ENEMY_ON_PLAY)) {
            checkDestroyOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, server);
        }
        if (playedCard.hasAction(CardRepository.Action.FREEZE_ADJACENT_CARDS_ON_PLAY)) {
            freezeEnemyAndAdjacentOnesOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, server);
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

    public static void checkEnemyDamageOnPlay(HashMap<String, List<Card>> allPlayerCards, HashMap<String, List<Card>> allTargetedCards,
                                              JSONObject message, Card playedCard, JSONObject responseDamaged, JSONObject responseOther,
                                              GameRoom room, GameServer.Client attackerPlayer) {
        Card damagedCard = allTargetedCards.get("field").get(message.getInt("opponent_pos"));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.FireElemental.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.B_30.getId()) {
            damagedCard.decreaseHp(playedCard.getCardInfo().getDamage());
            if (playedCard.hasAction(CardRepository.Action.DEAL_ENERGY_DMG)) {
                CardRepository.Status previous = damagedCard.getCurrentAlignedStatus();
                List<Integer> attackedIndexes = new ArrayList<>(List.of(message.getInt("opponent_pos")));
                if (previous != null) {
                    AlignmentUtil.onAlignedStatusApply(playedCard, damagedCard, allTargetedCards.get("field"), attackedIndexes,
                            previous, CardRepository.Status.ENERGY, attackerPlayer, room);
                }
                CardUtil.putFieldChanges(responseOther, allPlayerCards.get("field"), allTargetedCards.get("field"),
                        List.of(allPlayerCards.get("field").indexOf(playedCard)), attackedIndexes);
                CardUtil.putFieldChanges(responseDamaged, allTargetedCards.get("field"), allPlayerCards.get("field"),
                        attackedIndexes, List.of(allPlayerCards.get("field").indexOf(playedCard)));
            }
            else CardUtil.putDamagedCardInfo(damagedCard, message.getInt("opponent_pos"), responseDamaged, responseOther);
        }
    }

    public static void checkDamageOnPlay(HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards,
                                         Card playedCard, JSONObject player1Response, JSONObject player2Response, GameServer.Client client, GameServer.Client player1) {
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
                                         GameServer server) {
        List<CardRepository.Faction> factions = playedCard.getCardInfo().getFactions();
        for (CardRepository.Faction faction : factions) {
            switch (faction) {
                case STONE -> CardUtil.decreaseCost(CardRepository.CardTemplate.StoneGiant, allCards, player, server);
                case ELEMENTAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.WaterGiant, allCards, player, server);
                case ANIMAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.AnimalKing, allCards, player, server);
                case ROBOT -> CardUtil.decreaseCost(CardRepository.CardTemplate.STAN_3000, allCards, player, server);
            }
        }
        if (playedCard.hasKeyWord(CardRepository.KeyWord.GIANT)) {
            CardUtil.decreaseCost(CardRepository.CardTemplate.Deity, allCards, player, server);
        }
    }
}
