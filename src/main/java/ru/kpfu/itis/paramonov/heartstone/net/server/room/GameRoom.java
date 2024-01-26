package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.ServerResponse;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.util.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class GameRoom {

    public enum RoomAction {
        GET_INITIAL_INFO, DRAW_CARD, DRAW_CARD_OPPONENT, BEGIN_TURN, END_TURN, PLAY_CARD, PLAY_CARD_OPPONENT, CARD_CARD_ATTACK,
        GET_OPPONENT_MANA, CHECK_CARD_PLAYED, CARD_HERO_ATTACK, CHECK_CARD_TO_ATTACK, GAME_END, CHANGE_HP, GET_CHANGE, TIMER_UPDATE,
        ADD_CARDS_TO_HAND
    }

    private final GameServer.Client player1;

    private final GameServer.Client player2;

    private GameServer.Client activePlayer;

    private final GameServer server;

    private PlayerData dataPlayer1;

    private PlayerData dataPlayer2;

    private Thread player1Timer;

    private Thread player2Timer;

    Random random = new Random();

    public GameRoom(GameServer.Client player1, GameServer.Client player2, GameServer server) {
        this.player1 = player1;
        this.player2 = player2;
        this.server = server;
    }

    public void onStart() {
        initPlayerData();
        sendInitialInfo();
        setActivePlayer();
    }

    private void initPlayerData() {
        dataPlayer1 = new PlayerData();
        dataPlayer2 = new PlayerData();
    }

    public GameServer.Client getNonActivePlayer() {
        if (activePlayer.equals(player1)) return player2;
        else return player1;
    }

    public GameServer.Client getOtherPlayer(GameServer.Client player) {
        if (player.equals(player1)) return player2;
        else return player1;
    }
    public PlayerData getPlayerData(GameServer.Client player) {
        if (player.equals(player1)) return dataPlayer1;
        else return dataPlayer2;
    }

    private Thread getTimer(GameServer.Client player) {
        if (player.equals(player1)) return player1Timer;
        else return player2Timer;
    }

    public void handleMessage(JSONObject msg, GameServer.Client player) {
        switch (RoomAction.valueOf(msg.getString("room_action"))) {
            case END_TURN -> {
                ServerResponse responseEnd = new ServerResponse();
                responseEnd.putAction(RoomAction.END_TURN);
                if (!player.equals(activePlayer)) {
                    responseEnd.putStatus("not_ok");
                    responseEnd.putReason("Not your turn!");
                    server.sendResponse(responseEnd, player);
                    return;
                }
                getTimer(player).interrupt();
                activePlayer = getOtherPlayer(player);
                CardUtil.changeCardAbilityToAttackOnTurnEnd(getPlayerData(player), player,
                        getOtherPlayer(player), this);
                CardUtil.checkEndTurnCards(player, this);
                responseEnd.putStatus("ok");
                sendResponse(responseEnd, player);

                ServerResponse responseBegin = new ServerResponse();
                responseBegin.putAction(RoomAction.BEGIN_TURN);
                responseBegin.putStatus("ok");

                Hero resHero =
                        ManaUtil.increaseMana(responseBegin, getPlayerData(activePlayer));
                sendResponse(responseBegin, activePlayer);

                ServerResponse responseOpponentMana = new ServerResponse();
                ManaUtil.getOpponentMana(responseOpponentMana, resHero);
                sendResponse(responseOpponentMana, getNonActivePlayer());
                launchTimer(activePlayer);
                drawCard(activePlayer);
            }

            case CHECK_CARD_PLAYED -> {
                ServerResponse response = new ServerResponse();
                response.putAction(RoomAction.CHECK_CARD_PLAYED);
                if (player != activePlayer) {
                    response.putStatus("not_ok");
                    response.putReason("Not your turn!");
                    sendResponse(response, player);
                    return;
                }
                if (!checkCardIdFrom(msg, getPlayerData(player).getHand(), msg.getInt("hand_pos"), false)) {
                    putInvalidDataAndSendResponse(response, player);
                    return;
                }
                int mana = getPlayerData(player).getHero().getMana();

                CardOnPlayedUtil.checkCardPlayed(response, getPlayerData(player), msg.getInt("hand_pos"), mana);
                try {
                    String action = msg.getString("card_action");
                    response.putParameter("card_action", msg.getString("card_action"));
                    switch (action) {
                        case "target_enemy_card" -> response.putParameter("opponent_pos", msg.getInt("opponent_pos"));
                    }
                } catch (JSONException ignored) {}
                List<CardRepository.Status> initialStatuses = new ArrayList<>();
                Card card = getPlayerData(player).getHand().get(msg.getInt("hand_pos"));
                if (card.hasAction(CardRepository.Action.SHIELD_ON_PLAY)) {
                    initialStatuses.add(CardRepository.Status.SHIELDED);
                }
                response.putStatuses(initialStatuses);
                sendResponse(response, player);
            }

            case PLAY_CARD -> {
                ServerResponse opponentResponse = new ServerResponse();
                Card playedCard = CardOnPlayedUtil.addCardOnFieldWhenCardPlayed(msg.getInt("pos"), getPlayerData(player));
                List<CardRepository.Status> initialStatuses = new ArrayList<>();
                if (playedCard.hasAction(CardRepository.Action.CANNOT_ATTACK_ON_PLAY))
                    playedCard.addStatus(CardRepository.Status.CANNOT_ATTACK);
                if (playedCard.hasAction(CardRepository.Action.SHIELD_ON_PLAY)) {
                    playedCard.addStatus(CardRepository.Status.SHIELDED);
                    initialStatuses.add(CardRepository.Status.SHIELDED);
                }
                getPlayerData(player).getPlayedCards().add(playedCard);

                Hero hero = getPlayerData(player).getHero();
                int newMana = hero.getMana() - playedCard.getCost();
                hero.setMana(newMana);
                opponentResponse.putAction(RoomAction.PLAY_CARD_OPPONENT);
                opponentResponse.putStatus("ok");
                opponentResponse.putCardStatsAndId(playedCard);
                opponentResponse.putStatuses(initialStatuses);
                opponentResponse.putParameter("opponent_mana", newMana);
                opponentResponse.putParameter("opponent_hand_size", getPlayerData(player).getHand().size());
                sendResponse(opponentResponse, getOtherPlayer(player));

                ServerResponse response = new ServerResponse();
                response.putAction(RoomAction.PLAY_CARD);
                response.putParameter("pos", msg.getInt("pos"));
                response.putStatuses(initialStatuses);
                response.putParameter("mana", newMana);
                sendResponse(response, player);

                CardOnPlayedUtil.checkOnCardPlayed(player, getPlayerData(player), playedCard, this);

                if (playedCard.hasKeyWord(CardRepository.KeyWord.BATTLE_CRY)) {
                    try {
                        msg.getString("card_action");
                        ServerResponse responsePlayer = new ServerResponse();
                        ServerResponse responsePlayerTargeted = new ServerResponse();
                        CardOnPlayedUtil.checkBattleCry(player, playedCard, msg, responsePlayer, responsePlayerTargeted, this);
                    } catch (JSONException ignored) {}
                }
            }

            case CHECK_CARD_TO_ATTACK -> {
                ServerResponse response = new ServerResponse();
                Map<String, List<Card>> allCards = getPlayerData(player).getAllCards();
                String target = msg.getString("target");
                int pos = msg.getInt("pos");
                if(player != activePlayer) {
                    response.putAction(RoomAction.CHECK_CARD_TO_ATTACK);
                    response.putStatus("ok");
                    response.putReason("Not your turn");
                    sendResponse(response, player);
                    return;
                }
                if (!checkCardIdFrom(msg, allCards.get("field"), pos, false)) {
                    response.putAction(RoomAction.CHECK_CARD_TO_ATTACK);
                    putInvalidDataAndSendResponse(response, player);
                    return;
                };
                if (target.equals("hero")) {
                    boolean res = CardAttackUtil.checkCardToAttack(player, activePlayer, response,
                            allCards.get("field").get(pos), pos, target);
                    if (!res) {
                        response.putStatus("not_ok");
                        response.putReason("The card cannot attack right now");
                    }
                    else {
                        if (!allCards.get("field").get(pos).hasAction(CardRepository.Action.IGNORE_TAUNT)) {
                            List<Integer> positions = CardAttackUtil.checkTaunts(getPlayerData(getOtherPlayer(player)));
                            if (positions.size() > 0) {
                                response.putStatus("not_ok");
                                response.putReason("You must attack card with taunt");
                                res = false;
                            }
                        }
                        if (allCards.get("field").get(pos).hasStatus(CardRepository.Status.CAN_ATTACK_CARDS_ON_PLAY) && res) {
                            response.putStatus("not_ok");
                            response.putReason("Cards with board cannot attack heroes immediately");
                            res = false;
                        }
                        if (res) response.putStatus("ok");
                    }
                } else {
                    int opponentPos = msg.getInt("opponent_pos");
                    if (!checkCardIdFrom(msg, getPlayerData(getOtherPlayer(player)).getField(), opponentPos, true)) {
                        response.putAction(RoomAction.CHECK_CARD_TO_ATTACK);
                        putInvalidDataAndSendResponse(response, player);
                        return;
                    }
                    CardAttackUtil.checkCardToAttack(player, activePlayer, getPlayerData(getOtherPlayer(activePlayer)),
                            response, allCards.get("field").get(pos), pos, opponentPos, target);
                }
                if (response.getStatus().equals("not_ok")) sendResponse(response, player);
                else {
                    if (target.equals("hero")) onCardHeroAttack(msg, player);
                    else onCardCardAttack(msg, player);
                }
            }
        }
    }

    private boolean checkCardIdFrom(JSONObject msg, List<Card> from, int pos, boolean isOpponent) {
        int realId = from.get(pos).getCardInfo().getId();
        if (!isOpponent) {
            return (realId == msg.getInt("card_id"));
        } else {
            return (realId == msg.getInt("opponent_card_id"));
        }
    }

    private void putInvalidDataAndSendResponse(ServerResponse response, GameServer.Client player) {
        response.putStatus("not_ok");
        response.putReason("There is something wrong with your client. Consider quitting");
        sendResponse(response, player);
    }

    private void onCardCardAttack(JSONObject msg, GameServer.Client player) {
        GameServer.Client otherPlayer = getOtherPlayer(player);
        List<Card> attackerField = getPlayerData(player).getField();
        List<Card> attackedField = getPlayerData(otherPlayer).getField();
        int attackerPos = msg.getInt("pos");
        int attackedPos = msg.getInt("opponent_pos");

        Card attacker = attackerField.get(attackerPos);
        Card attacked = attackedField.get(attackedPos);
        attacker.addStatus(CardRepository.Status.ATTACKED);
        CardAttackUtil.decreaseHpOnDirectAttack(attacker, attacked);

        ServerResponse attackerResponse = new ServerResponse();
        ServerResponse attackedResponse = new ServerResponse();
        List<Integer> attackerIndexes = new ArrayList<>(List.of(attackerPos));
        List<Integer> attackedIndexes = new ArrayList<>(List.of(attackedPos));
        CardAttackUtil.checkAttackSpecialEffects(attacker, attacked, attackerIndexes, attackedIndexes,
                attackerResponse, attackedResponse, player, this);

        CardAttackUtil.putCardCardAttackAnimationInfo(attackerResponse, attackerPos, attackedPos, "attacker");
        attackerResponse.putFieldChanges(attackerField, attackedField, attackerIndexes, attackedIndexes);
        sendResponse(attackerResponse, player);

        CardAttackUtil.putCardCardAttackAnimationInfo(attackedResponse, attackedPos, attackerPos, "attacked");
        attackedResponse.putFieldChanges(attackedField, attackerField, attackedIndexes, attackerIndexes);
        sendResponse(attackedResponse, getOtherPlayer(player));

        CardUtil.removeDefeatedCards(attackerField);
        CardUtil.removeDefeatedCards(attackedField);
    }

    private void onCardHeroAttack(JSONObject msg, GameServer.Client player) {
        PlayerData playerData = getPlayerData(player);
        Card attacker = playerData.getField().get(msg.getInt("pos"));
        Hero attackedHero = getPlayerData(player).getHero();
        ServerResponse responseAttacker = new ServerResponse();
        ServerResponse responseAttacked = new ServerResponse();
        PlayerRoomUtil.onHeroAttacked(responseAttacker, responseAttacked, attackedHero, attacker, msg.getInt("pos"));

        sendResponse(responseAttacker, player);
        sendResponse(responseAttacked, getOtherPlayer(player));
        attacker.addStatus(CardRepository.Status.ATTACKED);

        if (attackedHero.getHp() <= 0) {
            onPlayerDefeated();
        }
    }

    private final int TIMER_MAX_SECONDS = 60;
    private void launchTimer(GameServer.Client client) {
        Runnable timerLogic = () -> {
            int seconds = 0;
            try {
                while(seconds < TIMER_MAX_SECONDS) {
                    Thread.sleep(1000);
                    seconds++;
                    ServerResponse response = new ServerResponse();
                    response.putAction(RoomAction.TIMER_UPDATE);
                    response.putStatus(String.valueOf(seconds));
                    response.putParameter("maxSeconds", TIMER_MAX_SECONDS);
                    server.sendResponse(response, client);
                }
                if (!Thread.currentThread().isInterrupted()) {
                    ServerResponse response = new ServerResponse();
                    response.putAction(RoomAction.TIMER_UPDATE);
                    response.putStatus("end");
                    server.sendResponse(response, client);
                }
            } catch (InterruptedException ignored) {}
        };
        if (client.equals(player1)) {
            player1Timer = new Thread(timerLogic);
            player1Timer.start();
        }
        else {
            player2Timer = new Thread(timerLogic);
            player2Timer.start();
        }
    }

    public void drawCard(GameServer.Client player) {
        ServerResponse response = new ServerResponse();
        response.putAction(RoomAction.DRAW_CARD);
        Card cardToDraw;

        PlayerData playerData = getPlayerData(player);
        try {
            cardToDraw = playerData.getDeck().remove(0);
            if (playerData.getHand().size() >= HAND_SIZE) {
                response.putParameter("card_status", "burned");
            } else {
                playerData.getHand().add(cardToDraw);
                CardUtil.checkCardEffectsOnCardDrawn(playerData.getField(), player, getOtherPlayer(player), this);
                response.putParameter("card_status", "drawn");
            }
            CardUtil.putCardInfo(cardToDraw, response);
        } catch (IndexOutOfBoundsException e) {
            response.putParameter("card_status", "no_card");
            ServerResponse responsePlayerDamaged = new ServerResponse();
            ServerResponse responsePlayerOther = new ServerResponse();
            playerData.incrementBurntCardDamage();
            PlayerRoomUtil.dealDamageOnNoCard(playerData, responsePlayerDamaged, responsePlayerOther);
            sendResponse(responsePlayerDamaged, player);
            sendResponse(responsePlayerOther, getOtherPlayer(player));

            if (dataPlayer1.getHero().getHp() <= 0 || dataPlayer2.getHero().getHp() <= 0) {
                onPlayerDefeated();
            }
        }
        response.putParameter("deck_size", playerData.getDeck().size());
        response.putStatus("ok");
        sendResponse(response, player);

        ServerResponse opponentResponse = new ServerResponse();
        opponentResponse.putAction(RoomAction.DRAW_CARD_OPPONENT);
        opponentResponse.putParameter("opponent_hand_size", playerData.getHand().size());
        sendResponse(opponentResponse, getOtherPlayer(player));
    }

    private void onPlayerDefeated() {
        ServerResponse responseWinner = new ServerResponse();
        ServerResponse responseDefeated = new ServerResponse();
        GameServer.Client winner;
        if (dataPlayer1.getHero().getHp() <= 0) winner = player2;
        else winner = player1;
        GameServer.Client defeated = getOtherPlayer(winner);
        PlayerRoomUtil.onHeroDefeated(responseWinner, responseDefeated, winner, defeated);
        end();
        sendResponses(responseWinner, responseDefeated, winner, defeated);
    }

    private void sendResponse(ServerResponse response, GameServer.Client client) {
        server.sendResponse(response, client);
    }

    private void setActivePlayer() {
        if (random.nextBoolean()) activePlayer = player1;
        else activePlayer = player2;

        drawCard(activePlayer);

        ServerResponse response = new ServerResponse();
        response.putAction(RoomAction.BEGIN_TURN);
        response.putStatus("ok");
        launchTimer(activePlayer);

        Hero resHero =
                ManaUtil.increaseMana(response, getPlayerData(activePlayer));
        sendResponse(response, activePlayer);

        ServerResponse responseOpponentMana = new ServerResponse();
        ManaUtil.getOpponentMana(responseOpponentMana, resHero);
        sendResponse(responseOpponentMana, getNonActivePlayer());
    }

    private final int BACKGROUND_AMOUNT = 9;
    private void sendInitialInfo() {
        setInitialInfo(player1);
        setInitialInfo(player2);

        ServerResponse player1Response = new ServerResponse();
        ServerResponse player2Response = new ServerResponse();

        player1Response.putHpInfo(dataPlayer1.getHero().getHp(), dataPlayer2.getHero().getHp());
        player2Response.putHpInfo(dataPlayer2.getHero().getHp(), dataPlayer1.getHero().getHp());

        int randomBg = random.nextInt(1, BACKGROUND_AMOUNT + 1);
        putInitialInfo(player1Response, player2Response, dataPlayer1.getDeck(), player1, randomBg);
        putInitialInfo(player2Response, player1Response, dataPlayer2.getDeck(), player2, randomBg);

        sendResponses(player1Response, player2Response, player1, player2);
    }

    private void setInitialInfo(GameServer.Client player) {
        PlayerData playerData = getPlayerData(player);
        playerData.setDeck(getShuffledDeck(player));
        CardUtil.makeCardsUnableToAttackOnStart(playerData.getDeck());
        int initialHp = PlayerRoomUtil.getInitialHp(playerData.getDeck());
        playerData.setHero(new Hero(initialHp, initialHp, 0, 0));
    }

    private final int INITIAL_HAND_SIZE = 3;

    public static final int HAND_SIZE = 7;

    private void putInitialInfo(ServerResponse response, ServerResponse otherResponse,
                                List<Card> deck, GameServer.Client player, int background) {
        response.putAction(RoomAction.GET_INITIAL_INFO);
        JSONArray arrayHand = new JSONArray();

        int counter = 1;
        List<Card> handCards = new ArrayList<>();

        for (Card card : deck) {
            if (counter <= INITIAL_HAND_SIZE) {
                handCards.add(card);
                CardUtil.putCardInfo(card, arrayHand);
            }
            counter++;
        }
        deck.removeAll(handCards);
        getPlayerData(player).setHand(handCards);

        response.putParameter("hand", arrayHand);
        response.putParameter("deck_size", deck.size());
        response.putParameter("background", "bg_" + background + ".png");

        otherResponse.putParameter("opponent_hand_size", arrayHand.length());
    }

    private List<Card> getShuffledDeck(GameServer.Client client) {
        String clientDeck = getClientDeckCardIds(client);
        List<CardRepository.CardTemplate> clientDeckList = CardRepository.getCardsById(clientDeck);
        List<Card> res = clientDeckList.stream()
                .map(cardTemplate -> new Card(cardTemplate))
                .collect(Collectors.toList());

        Collections.shuffle(res);
        return res;
    }

    private String getClientDeckCardIds(GameServer.Client client) {
        UserService service = new UserService();
        String clientLogin = client.getUserLogin();
        String clientCardIdsString = service.get(clientLogin).deck();

        return clientCardIdsString;
    }

    public void notifyDisconnected(GameServer.Client client) {
        ServerResponse response = new ServerResponse();
        response.putAction(RoomAction.GAME_END);
        response.putResult("win");
        end();
        try {
            PlayerRoomUtil.updateUsers(response, getOtherPlayer(client), client);
        } catch (SQLException e) {}
        sendResponse(response, getOtherPlayer(client));
    }

    public void putRoomAction(ServerResponse responsePlayer, ServerResponse responseOtherPlayer, RoomAction action) {
        responsePlayer.putAction(action);
        responseOtherPlayer.putAction(action);
    }

    public void sendResponses(ServerResponse responsePlayer, ServerResponse responseOtherPlayer, GameServer.Client player,
                              GameServer.Client otherPlayer) {
        server.sendResponse(responsePlayer, player);
        server.sendResponse(responseOtherPlayer, otherPlayer);
    }

    public GameServer getServer() {
        return server;
    }

    private void end() {
        player1.notifyGameEnd();
        player2.notifyGameEnd();
        dataPlayer1.clear();
        dataPlayer2.clear();
        try {
            player1Timer.interrupt();
        } catch (NullPointerException ignored) {}
        try {
            player2Timer.interrupt();
        } catch (NullPointerException ignored) {}
        activePlayer = null;
    }
}
