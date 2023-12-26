package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

import java.util.*;

public class CardUtil {
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

    public static void checkCardPlayed(JSONObject response, GameServer.Client client, GameServer.Client activePlayer, GameServer.Client player1, int handPos,
                                          HashMap<String, List<Card>> player1AllCards, HashMap<String, List<Card>> player2AllCards, int mana) {
        HashMap<String, List<Card>> allCards = getAllCards(client, player1, player1AllCards, player2AllCards);
        if (!client.equals(activePlayer)) {
            response.put("status", "not_ok");
            response.put("reason", "Not your turn");
            return;
        }
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
            if (!attacker.getCardInfo().getActions().contains(CardRepository.CardAction.IGNORE_TAUNT)) {
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
                                      JSONObject responsePlayer1, JSONObject responsePlayer2, GameServer server, GameRoom room) {
        responsePlayer1.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        responsePlayer2.put("room_action", GameRoom.RoomAction.GET_CHANGE);
        if (client.equals(player1)) {
            checkTargetedCardsOnBattleCry(player1, player2, player2AllCards, playedCard, message, responsePlayer1, responsePlayer2, server);
        }
        else {
            checkTargetedCardsOnBattleCry(player2, player1, player1AllCards, playedCard, message, responsePlayer2, responsePlayer1, server);
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DRAW_CARD_ON_PLAY)) {
            if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Dragon.getId()) {
                room.drawCard(client);
            }
            if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Illusionist.getId()) {
                room.drawCard(client);
                room.drawCard(client);
                room.drawCard(room.getOtherPlayer(client));
                room.drawCard(room.getOtherPlayer(client));
            }
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DAMAGE_ON_PLAY)) {
            checkDamageOnPlay(player1AllCards, player2AllCards, playedCard, responsePlayer1, responsePlayer2, client, player1);
            sendResponses(responsePlayer1, responsePlayer2, player1, player2, server);
        }
        removeDefeatedCards(player1AllCards.get("field"));
        removeDefeatedCards(player2AllCards.get("field"));
    }

    public static void checkTargetedCardsOnBattleCry(GameServer.Client player, GameServer.Client targetedPlayer, HashMap<String, List<Card>> allTargetedCards,
                                                     Card playedCard, JSONObject message, JSONObject response,
                                                     JSONObject responseTargeted, GameServer server) {
        if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.FREEZE_ENEMY_ON_PLAY)) {
            freezeEnemyOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            sendResponses(response, responseTargeted, player, targetedPlayer, server);
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DAMAGE_ENEMY_ON_PLAY)) {
            checkEnemyDamageOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            sendResponses(response, responseTargeted, player, targetedPlayer, server);
        }
        if (playedCard.getCardInfo().getActions().contains(CardRepository.CardAction.DESTROY_ENEMY_ON_PLAY)) {
            checkDestroyOnPlay(allTargetedCards, message, playedCard, responseTargeted, response);
            sendResponses(response, responseTargeted, player, targetedPlayer, server);
        }
    }

    public static void checkProfessorDogOnCardDraw(List<Card> field, GameServer.Client player, GameServer.Client other, GameServer server) {
        for (Card card : field) {
            if (card.getCardInfo().getId() == CardRepository.CardTemplate.ProfessorDog.getId()) {
                card.setAtk(card.getAtk() + card.getCardInfo().getAtkIncrease());
                card.setHp(card.getHp() + card.getCardInfo().getHpIncrease());
                JSONObject response = new JSONObject();
                JSONObject otherResponse = new JSONObject();
                response.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                otherResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE);
                response.put("pos", field.indexOf(card));
                response.put("atk", card.getAtk());
                response.put("hp", card.getHp());

                otherResponse.put("opponent_pos", field.indexOf(card));
                otherResponse.put("atk", card.getAtk());
                otherResponse.put("hp", card.getHp());

                server.sendResponse(response.toString(), player);
                server.sendResponse(otherResponse.toString(), other);
            }
        }
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

    public static void freezeEnemyOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard, JSONObject responseTargeted, JSONObject responseOther) {
        Card frozenCard = allTargetedCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.IceElemental.getId()) {
            frozenCard.addStatus(CardRepository.Status.FROZEN_1);
            putFrozenCardInfo(Integer.parseInt(message.getString("opponent_pos")), responseTargeted, responseOther, false);
        }
    }

    public static void checkEnemyDamageOnPlay(HashMap<String, List<Card>> allTargetedCards, JSONObject message, Card playedCard,
                                              JSONObject responseDamaged, JSONObject responseOther) {
        Card damagedCard = allTargetedCards.get("field").get(Integer.parseInt(message.getString("opponent_pos")));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.FireElemental.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.B_30.getId()) {
            damagedCard.setHp(damagedCard.getHp() - playedCard.getCardInfo().getDamage());
            putDamagedCardInfo(damagedCard, Integer.parseInt(message.getString("opponent_pos")), responseDamaged, responseOther);
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
                playedCard.setAtk(playedCard.getAtk() + playedCard.getCardInfo().getAtkIncrease() * defeatedAmount);
                playedCard.setHp(playedCard.getHp() + playedCard.getCardInfo().getHpIncrease() * defeatedAmount);
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
                        if (card.getCardInfo().getId() == CardRepository.CardTemplate.CrazyPyromaniac.getId()) {
                            card.setHp(card.getHp() - card.getCardInfo().getAllCardsDamage());
                            playerIndexes.add(field.indexOf(card));
                        }
                        if (card.getCardInfo().getId() == CardRepository.CardTemplate.Trantos.getId()) {
                            card.setHp(card.getHp() - card.getCardInfo().getAllCardsDamage());
                            playerIndexes.add(field.indexOf(card));
                        }
                    }
                }
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
        for (Integer position : positions) {
            JSONObject changedCard = new JSONObject();
            Card card = field.get(position);
            changedCard.put("pos", position);
            changedCard.put("hp", card.getHp());
            changedCard.put("atk", card.getAtk());

            changes.put(changedCard);
        }
        return changes;
    }

    public static void putOpponentChanges(JSONObject response, List<Card> field, List<Integer> positions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("opponent_stat_changes", changes);
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
        playerAllCards.get("field").stream()
                .filter(card -> card.getCardInfo().getId() == CardRepository.CardTemplate.HypnoShroom.getId())
                .forEach(card -> {
                    JSONObject responseOtherPlayer = new JSONObject();
                    JSONObject responsePlayer = new JSONObject();
                    List<List<Card>> res = checkHypnoshroom(card, playerAllCards, otherPlayerAllCards, responsePlayer, responseOtherPlayer);
                    if (res == null) return;
                    removed.addAll(res.get(0));
                    added.addAll(res.get(1));
                    server.sendResponse(responseOtherPlayer.toString(), otherPlayer);
                    server.sendResponse(responsePlayer.toString(), player);
                });
        otherPlayerAllCards.get("field").removeAll(removed);
        playerAllCards.get("field").addAll(added);
    }

    private static List<List<Card>> checkHypnoshroom(Card card, HashMap<String, List<Card>> playerCards, HashMap<String, List<Card>> otherPlayerCards,
                                                     JSONObject playerResponse, JSONObject otherPlayerResponse) {
        List<Card> removed = new ArrayList<>();
        List<Card> added = new ArrayList<>();
        if (card.getCardInfo().getId() == CardRepository.CardTemplate.HypnoShroom.getId()) {
            playerResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE.toString());
            otherPlayerResponse.put("room_action", GameRoom.RoomAction.GET_CHANGE.toString());
            Random random = new Random();
            if (otherPlayerCards.get("field").size() == 0 || playerCards.get("field").size() >= MAX_FIELD_SIZE) return null;
            int randPos = random.nextInt(otherPlayerCards.get("field").size());
            Card stolenCard = otherPlayerCards.get("field").get(randPos);

            added.add(stolenCard);
            removed.add(stolenCard);

            playerResponse.put("gotten_pos", randPos);
            putCardStatsAndId(stolenCard, playerResponse);

            otherPlayerResponse.put("stolen_pos", randPos);
            putCardStatsAndId(stolenCard, otherPlayerResponse);

        }
        return List.of(removed, added);
    }

    public static void checkAttackSpecialEffects(Card attacker, Card attacked, List<Card> attackerField,
                                                 List<Card> attackedField, Hero attackerHero, Hero attackedHero,
                                                 JSONObject attackerResponse, JSONObject attackedResponse,
                                                 GameServer.Client attackerPlayer, GameServer.Client attackedPlayer) {
        if (attacked.getCardInfo().getId() == CardRepository.CardTemplate.TheRock.getId() && attacked.getHp() > 0) {
            dealHeroDamageOnPunishment(attackerHero, attackedHero, attacked.getAtk(), attackerResponse, attackedResponse,
                    attackerPlayer, attackedPlayer);
            attacked.setAtk(attacked.getAtk() + attacked.getCardInfo().getAtk());
            attacked.setHp(attacked.getHp() + attacked.getCardInfo().getHp());
        }
        if (attacked.getHp() > 0 && (attacked.getCardInfo().getId() == CardRepository.CardTemplate.SlimeCommander.getId() ||
                attacked.getCardInfo().getId() == CardRepository.CardTemplate.HeartStone.getId())) {
            dealHeroDamageOnPunishment(attackerHero, attackedHero, attacked.getCardInfo().getHeroDamage(),
                    attackerResponse, attackedResponse, attackerPlayer, attackedPlayer);
        }
    }

    private static void dealHeroDamageOnPunishment(Hero attackerHero, Hero attackedHero, int punishmentDamage,
                                                   JSONObject attackerResponse, JSONObject attackedResponse,
                                                   GameServer.Client attackerPlayer, GameServer.Client attackedPlayer) {
        attackerHero.setHp(attackerHero.getHp() - punishmentDamage);
        if (attackerHero.getHp() <= 0) {
            PlayerRoomUtil.onHeroDefeated(attackedResponse, attackerResponse, attackedPlayer, attackerPlayer);
        }
        PlayerRoomUtil.putHpInfo(attackerResponse, attackerHero.getHp(), attackedHero.getHp());
        PlayerRoomUtil.putHpInfo(attackedResponse, attackedHero.getHp(), attackerHero.getHp());
    }

    public static void checkOnCardPlayed(GameServer.Client player, HashMap<String, List<Card>> allCards, Card playedCard,
                                         GameServer server) {
        CardRepository.Faction faction = playedCard.getCardInfo().getFaction();
        switch (faction) {
            case STONE -> decreaseCost(CardRepository.CardTemplate.StoneGiant.getId(), 1, allCards, player, server);
            case ELEMENTAL -> decreaseCost(CardRepository.CardTemplate.WaterGiant.getId(), 1, allCards, player, server);
            case ANIMAL -> decreaseCost(CardRepository.CardTemplate.AnimalKing.getId(), 1, allCards, player, server);
            case ROBOT -> decreaseCost(CardRepository.CardTemplate.STAN_3000.getId(), 1, allCards, player, server);
        }
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.AnimalKing.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.WaterGiant.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.StoneGiant.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.STAN_3000.getId()) {
            decreaseCost(CardRepository.CardTemplate.Deity.getId(), 6, allCards, player, server);
        }
    }

    public static void decreaseCost(int cardId, int costDecrement, HashMap<String, List<Card>> allCards,
                                    GameServer.Client player, GameServer server) {
        for (Card card : allCards.get("hand")) {
            if (card.getCardInfo().getId() == cardId) {
                decreaseCostAndSend(costDecrement, player, allCards, server, card);
            }
        }
        for (Card card : allCards.get("deck")) {
            if (card.getCardInfo().getId() == cardId) {
                card.setCost(card.getCost() - costDecrement);
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
