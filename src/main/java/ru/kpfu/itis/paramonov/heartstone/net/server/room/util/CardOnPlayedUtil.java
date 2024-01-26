package ru.kpfu.itis.paramonov.heartstone.net.server.room.util;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.ServerResponse;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.PlayerData;

import java.util.ArrayList;
import java.util.List;

public class CardOnPlayedUtil {
    public static Card addCardOnFieldWhenCardPlayed(int pos, PlayerData playerData) {
        List<Card> hand = playerData.getHand();
        Card card = hand.remove(pos);
        List<Card> field = playerData.getField();
        field.add(card);
        return card;
    }

    public static void checkCardPlayed(ServerResponse response, PlayerData playerData, int handPos, int mana) {
        if (playerData.getField().size() == CardUtil.MAX_FIELD_SIZE) {
            response.putStatus("not_ok");
            response.putReason("Full field");
        }
        else if (playerData.getHand().get(handPos).getCost() > mana) {
            response.putStatus("not_ok");
            response.putReason("Insufficient mana");
        }
        else {
            response.putParameter("hand_pos", handPos);
            response.putStatus("ok");
        }
    }
    public static void checkBattleCry(GameServer.Client player, Card playedCard, JSONObject message,
                                      ServerResponse responsePlayer, ServerResponse responseTargeted, GameRoom room) {
        room.putRoomAction(responsePlayer, responseTargeted, GameRoom.RoomAction.GET_CHANGE);
        GameServer.Client otherPlayer = room.getOtherPlayer(player);
        PlayerData playerData = room.getPlayerData(player);
        PlayerData otherPlayerData = room.getPlayerData(otherPlayer);
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
            checkAllCardsDamageOnPlay(playerData, otherPlayerData, playedCard, responsePlayer, responseTargeted);
            CardUtil.sendGetChangeResponses(responsePlayer, responseTargeted, player, otherPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.GIVE_SHIELD_ON_PLAY)) {
            List<Card> field = playerData.getField();
            List<Integer> playerIndexes = new ArrayList<>();
            checkGivenShield(field.indexOf(playedCard), playedCard, field, playerIndexes);
            responsePlayer.putFieldChanges(playerData.getField(), otherPlayerData.getField(), playerIndexes, List.of());
            responseTargeted.putFieldChanges(otherPlayerData.getField(), playerData.getField(), List.of(), playerIndexes);
            CardUtil.sendGetChangeResponses(responsePlayer, responseTargeted, player, room.getOtherPlayer(player), room);
        }
        CardUtil.removeDefeatedCards(playerData.getField());
        CardUtil.removeDefeatedCards(otherPlayerData.getField());
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
                                                     JSONObject message, ServerResponse response,
                                                     ServerResponse responseTargeted, GameRoom room) {
        GameServer.Client targetedPlayer = room.getOtherPlayer(player);
        PlayerData targetedPlayerData = room.getPlayerData(targetedPlayer);
        if (playedCard.hasAction(CardRepository.Action.FREEZE_ENEMY_ON_PLAY)) {
            freezeEnemyOnPlay(targetedPlayerData, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.DAMAGE_ENEMY_ON_PLAY)) {
            checkEnemyDamageOnPlay(player, message.getInt("opponent_pos"), playedCard, responseTargeted, response, room);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.DESTROY_ENEMY_ON_PLAY)) {
            checkDestroyOnPlay(targetedPlayerData, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
        if (playedCard.hasAction(CardRepository.Action.FREEZE_ADJACENT_CARDS_ON_PLAY)) {
            freezeEnemyAndAdjacentOnesOnPlay(targetedPlayerData, message, playedCard, responseTargeted, response);
            CardUtil.sendGetChangeResponses(response, responseTargeted, player, targetedPlayer, room);
        }
    }

    public static void freezeEnemyOnPlay(PlayerData playerData, JSONObject message, Card playedCard,
                                         ServerResponse responseTargeted, ServerResponse responseOther) {
        Card frozenCard = playerData.getField().get(message.getInt("opponent_pos"));
        frozenCard.addStatus(CardRepository.Status.FROZEN_1);
        CardUtil.putFrozenCardInfo(message.getInt("opponent_pos"), responseTargeted, responseOther, false);
    }

    public static void freezeEnemyAndAdjacentOnesOnPlay(PlayerData playerData, JSONObject message, Card playedCard,
                                                        ServerResponse responseTargeted, ServerResponse responseOther) {
        int frozenPos = message.getInt("opponent_pos");
        List<Integer> indexes = new ArrayList<>();
        indexes.add(frozenPos);
        Card frozenCard = playerData.getField().get(frozenPos);
        frozenCard.addStatus(CardRepository.Status.FROZEN_1);
        freezeAdjacentCards(playerData.getField(), frozenPos, indexes);
        responseOther.putIntegers("opponent_frozen_positions", indexes);
        responseTargeted.putIntegers("frozen_positions", indexes);
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
                                              ServerResponse responseTargeted, ServerResponse response, GameRoom room) {
        List<Card> playerField = room.getPlayerData(attackerPlayer).getField();
        List<Card> targetedField = room.getPlayerData(room.getOtherPlayer(attackerPlayer)).getField();
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
                response.putFieldChanges(playerField, targetedField, List.of(playerField.indexOf(playedCard)), attackedIndexes);
                responseTargeted.putFieldChanges(targetedField, playerField, attackedIndexes, List.of(playerField.indexOf(playedCard)));
            }
            else CardUtil.putDamagedCardInfo(damagedCard, opponentPos, responseTargeted, response);
        }
    }

    public static void checkAllCardsDamageOnPlay(PlayerData dataPlayer1, PlayerData dataPlayer2,
                                                 Card playedCard, ServerResponse player1Response, ServerResponse player2Response) {
        List<Integer> player1Indexes = new ArrayList<>();
        List<Integer> player2Indexes = new ArrayList<>();
        boolean isCardTrantos = playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Trantos.getId();
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.CrazyPyromaniac.getId() || isCardTrantos) {
            damageAllCardsExceptPlayedAndAdd(dataPlayer1.getField(), playedCard, player1Indexes, isCardTrantos);
            damageAllCardsExceptPlayedAndAdd(dataPlayer2.getField(), playedCard, player2Indexes, isCardTrantos);
            player1Response.putParameter("anim", "field_fire");
            player2Response.putParameter("anim", "field_fire");
            if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Trantos.getId()) {
                int defeatedAmount = CardUtil.getDefeatedCardAmount(dataPlayer1.getField()) +
                        CardUtil.getDefeatedCardAmount(dataPlayer2.getField());
                playedCard.setAtk(playedCard.getAtk() + playedCard.getCardInfo().getAtkIncrease() * defeatedAmount);
                playedCard.increaseMaxHp(playedCard.getCardInfo().getHpIncrease() * defeatedAmount);
                playedCard.increaseHp(playedCard.getCardInfo().getHpIncrease() * defeatedAmount);
            }
        }
        player1Response.putFieldChanges(dataPlayer1.getField(), dataPlayer2.getField(), player1Indexes, player2Indexes);
        player2Response.putFieldChanges(dataPlayer2.getField(), dataPlayer1.getField(), player2Indexes, player1Indexes);
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

    public static void checkDestroyOnPlay(PlayerData playerData, JSONObject message, Card playedCard,
                                          ServerResponse responseDestroyed, ServerResponse responseOther) {
        Card destroyedCard = playerData.getField().get(message.getInt("opponent_pos"));
        if (playedCard.getCardInfo().getId() == CardRepository.CardTemplate.StoneAssassin.getId() ||
                playedCard.getCardInfo().getId() == CardRepository.CardTemplate.Hydra.getId()) {
            destroyedCard.setHp(0);
            CardUtil.putDamagedCardInfo(destroyedCard, message.getInt("opponent_pos"), responseDestroyed, responseOther);
        }
    }

    public static void checkOnCardPlayed(GameServer.Client player, PlayerData playerData, Card playedCard,
                                         GameRoom room) {
        List<CardRepository.Faction> factions = playedCard.getCardInfo().getFactions();
        for (CardRepository.Faction faction : factions) {
            switch (faction) {
                case STONE -> CardUtil.decreaseCost(CardRepository.CardTemplate.StoneGiant, playerData, player, room);
                case ELEMENTAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.WaterGiant, playerData, player, room);
                case ANIMAL -> CardUtil.decreaseCost(CardRepository.CardTemplate.AnimalKing, playerData, player, room);
                case ROBOT -> CardUtil.decreaseCost(CardRepository.CardTemplate.STAN_3000, playerData, player, room);
            }
        }
        if (playedCard.hasKeyWord(CardRepository.KeyWord.GIANT)) {
            CardUtil.decreaseCost(CardRepository.CardTemplate.Deity, playerData, player, room);
        }
    }
}
