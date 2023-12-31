package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONObject;
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
        int pos = Integer.parseInt(msg.getString("pos"));
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
        if (allCards.get("field").size() == CardUtil.getMaxFieldSize()) {
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
        if (client.equals(player1)) {
            checkTargetedCardsOnBattleCry(player1, player2, player2AllCards, playedCard, message, responsePlayer1, responsePlayer2, server);
        }
        else {
            checkTargetedCardsOnBattleCry(player2, player1, player1AllCards, playedCard, message, responsePlayer2, responsePlayer1, server);
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.Action.DRAW_CARD_ON_PLAY)) {
            for (int i = 0; i < playedCard.getCardInfo().getDrawnCards(); i++) {
                room.drawCard(client);
                if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Illusionist.getId()) {
                    room.drawCard(room.getOtherPlayer(client));
                }
            }
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.Action.DAMAGE_ON_PLAY)) {
            checkDamageOnPlay(player1AllCards, player2AllCards, playedCard, responsePlayer1, responsePlayer2, client, player1);
            CardUtil.sendGetChangeResponses(responsePlayer1, responsePlayer2, player1, player2, server);
        }
        CardUtil.removeDefeatedCards(player1AllCards.get("field"));
        CardUtil.removeDefeatedCards(player2AllCards.get("field"));
    }

    public static void checkTargetedCardsOnBattleCry(GameServer.Client player, GameServer.Client targetedPlayer, HashMap<String, List<Card>> allTargetedCards,
                                                     Card playedCard, JSONObject message, JSONObject response,
                                                     JSONObject responseTargeted, GameServer server) {
        if (playedCard.getCardInfo().getActions().contains(CardRepository.Action.FREEZE_ENEMY_ON_PLAY)) {
            freezeEnemyOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, server);
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.Action.DAMAGE_ENEMY_ON_PLAY)) {
            checkEnemyDamageOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, server);
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.Action.DESTROY_ENEMY_ON_PLAY)) {
            checkDestroyOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, server);
        }
    }

    public static void freezeEnemyOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard,
                                         JSONObject responseTargeted, JSONObject responseOther) {
        Card frozenCard = allTargetedCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.IceElemental.getId()) {
            frozenCard.addStatus(CardRepository.Status.FROZEN_1);
            CardUtil.putFrozenCardInfo(Integer.parseInt(message.getString("opponent_pos")), responseTargeted, responseOther, false);
        }
    }

    public static void checkEnemyDamageOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard,
                                              JSONObject responseDamaged, JSONObject responseOther) {
        Card damagedCard = allTargetedCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.FireElemental.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.B_30.getId()) {
            damagedCard.setHp(damagedCard.getHp() - playedCard.getCardInfo().getDamage());
            CardUtil.putDamagedCardInfo(damagedCard, Integer.parseInt(message.getString("opponent_pos")), responseDamaged, responseOther);
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
            int defeatedAmount = CardUtil.getDefeatedCardAmount(player1AllCards.get("field")) +
                    CardUtil.getDefeatedCardAmount(player2AllCards.get("field"));
            playedCard.setAtk(playedCard.getAtk() + playedCard.getCardInfo().getAtkIncrease() * defeatedAmount);
            playedCard.setHp(playedCard.getHp() + playedCard.getCardInfo().getHpIncrease() * defeatedAmount);
        }
        CardUtil.putFieldChanges(player1Response, player1AllCards.get("field"), player1Indexes);
        CardUtil.putOpponentChanges(player1Response, player2AllCards.get("field"), player2Indexes);

        CardUtil.putOpponentChanges(player2Response, player1AllCards.get("field"), player1Indexes);
        CardUtil.putFieldChanges(player2Response, player2AllCards.get("field"), player2Indexes);
    }

    private static void damageAllCardsExceptPlayedAndAdd(List<Card> field, Card playedCard, List<Integer> playerIndexes, boolean addCard) {
        for (Card card : field) {
            if (!card.equals(playedCard)) {
                card.setHp(card.getHp() - playedCard.getCardInfo().getAllCardsDamage());
                playerIndexes.add(field.indexOf(card));
            } else {
                if (addCard) playerIndexes.add(field.indexOf(card));
            }
        }
    }

    public static void checkDestroyOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard,
                                          JSONObject responseDestroyed, JSONObject responseOther) {
        Card destroyedCard = allTargetedCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.StoneAssassin.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Hydra.getId()) {
            destroyedCard.setHp(0);
            CardUtil.putDamagedCardInfo(destroyedCard, Integer.parseInt(message.getString("opponent_pos")), responseDestroyed, responseOther);
        }
    }

    public static void checkOnCardPlayed(GameServer.Client player, HashMap<String, List<Card>> allCards, Card playedCard,
                                         GameServer server) {
        CardRepository.Faction faction = playedCard.getCardInfo().getFaction();
        switch (faction) {
            case STONE -> CardUtil.decreaseCost(CardRepository.CardTemplate.StoneGiant, allCards, player, server);
            case ELEMENTAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.WaterGiant, allCards, player, server);
            case ANIMAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.AnimalKing, allCards, player, server);
            case ROBOT -> CardUtil.decreaseCost(CardRepository.CardTemplate.STAN_3000, allCards, player, server);
        }
        if (playedCard.getCardInfo().getKeyWords().contains(CardRepository.KeyWord.GIANT)) {
            CardUtil.decreaseCost(CardRepository.CardTemplate.Deity, allCards, player, server);
        }
    }
}
