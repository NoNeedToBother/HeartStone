package ru.kpfu.itis.paramonov.heartstone.net.server.room;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;

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
    public GameServer.Client getActivePlayer() {
        return activePlayer;
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
        System.out.println(msg.toString());
        switch (RoomAction.valueOf(msg.getString("room_action"))) {
            case END_TURN -> {
                JSONObject responseEnd = new JSONObject();
                if (!player.equals(activePlayer)) {
                    responseEnd.put("status", "not_ok");
                    responseEnd.put("reason", "Not your turn!");
                    server.sendResponse(responseEnd.toString(), player);
                    return;
                }
                getTimer(player).interrupt();
                activePlayer = getOtherPlayer(player);
                CardUtil.changeCardAbilityToAttackOnTurnEnd(getPlayerData(player), player,
                        getOtherPlayer(player), this);
                CardUtil.checkEndTurnCards(player, this);
                responseEnd.put("room_action", RoomAction.END_TURN.toString());
                responseEnd.put("status", "ok");
                sendResponse(responseEnd.toString(), player);

                JSONObject responseBegin = new JSONObject();
                responseBegin.put("room_action", RoomAction.BEGIN_TURN.toString());
                responseBegin.put("status", "ok");

                Hero resHero =
                        ManaUtil.increaseMana(responseBegin, getPlayerData(activePlayer));
                sendResponse(responseBegin.toString(), activePlayer);

                JSONObject responseOpponentMana = new JSONObject();
                ManaUtil.getOpponentMana(responseOpponentMana, resHero);
                sendResponse(responseOpponentMana.toString(), getNonActivePlayer());
                launchTimer(activePlayer);
                drawCard(activePlayer);
            }

            case CHECK_CARD_PLAYED -> {
                JSONObject response = new JSONObject();
                response.put("room_action", RoomAction.CHECK_CARD_PLAYED.toString());
                if (player != activePlayer) {
                    response.put("room_action", RoomAction.CHECK_CARD_PLAYED);
                    response.put("status", "not_ok");
                    response.put("reason", "Not your turn");
                    sendResponse(response.toString(), player);
                    return;
                }
                if (!checkCardIdFrom(msg, getPlayerData(player).getHand(), msg.getInt("hand_pos"), false)) {
                    response.put("room_action", RoomAction.CHECK_CARD_PLAYED);
                    putInvalidDataAndSendResponse(response, player);
                    return;
                }
                int mana = getPlayerData(player).getHero().getMana();

                if (!player.equals(activePlayer)) {
                    response.put("status", "not_ok");
                    response.put("reason", "Not your turn");
                } else CardOnPlayedUtil.checkCardPlayed(response, getPlayerData(player), msg.getInt("hand_pos"), mana);
                try {
                    String action = msg.getString("card_action");
                    response.put("card_action", msg.getString("card_action"));
                    switch (action) {
                        case "target_enemy_card" -> response.put("opponent_pos", msg.getInt("opponent_pos"));
                    }
                } catch (JSONException ignored) {}
                List<CardRepository.Status> initialStatuses = new ArrayList<>();
                Card card = getPlayerData(player).getHand().get(msg.getInt("hand_pos"));
                if (card.hasAction(CardRepository.Action.SHIELD_ON_PLAY)) {
                    initialStatuses.add(CardRepository.Status.SHIELDED);
                }
                CardUtil.putStatuses(initialStatuses, response);
                sendResponse(response.toString(), player);
            }

            case PLAY_CARD -> {
                JSONObject opponentResponse = new JSONObject();
                Card playedCard = CardOnPlayedUtil.addCardOnFieldWhenCardPlayed(msg, getPlayerData(player));
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
                opponentResponse.put("room_action", RoomAction.PLAY_CARD_OPPONENT.toString());
                opponentResponse.put("status", "ok");
                CardUtil.putCardStatsAndId(playedCard, opponentResponse);
                CardUtil.putStatuses(initialStatuses, opponentResponse);
                opponentResponse.put("opponent_mana", newMana);
                opponentResponse.put("opponent_hand_size", getPlayerData(player).getHand().size());
                sendResponse(opponentResponse.toString(), getOtherPlayer(player));

                JSONObject response = new JSONObject();
                response.put("room_action", RoomAction.PLAY_CARD.toString());
                response.put("pos", msg.getInt("pos"));
                CardUtil.putStatuses(initialStatuses, response);
                response.put("mana", newMana);
                sendResponse(response.toString(), player);

                CardOnPlayedUtil.checkOnCardPlayed(player, getPlayerData(player), playedCard, this);

                if (playedCard.hasKeyWord(CardRepository.KeyWord.BATTLE_CRY)) {
                    try {
                        msg.getString("card_action");
                        JSONObject responsePlayer = new JSONObject();
                        JSONObject responsePlayerTargeted = new JSONObject();
                        CardOnPlayedUtil.checkBattleCry(player, playedCard, msg, responsePlayer, responsePlayerTargeted, this);
                    } catch (JSONException ignored) {}
                }
            }

            case CHECK_CARD_TO_ATTACK -> {
                JSONObject response = new JSONObject();
                Map<String, List<Card>> allCards = getPlayerData(player).getAllCards();
                String target = msg.getString("target");
                int pos = msg.getInt("pos");
                if(player != activePlayer) {
                    response.put("room_action", RoomAction.CHECK_CARD_TO_ATTACK);
                    response.put("status", "ok");
                    response.put("reason", "Not your turn");
                    sendResponse(response.toString(), player);
                    return;
                }
                if (!checkCardIdFrom(msg, allCards.get("field"), pos, false)) {
                    response.put("room_action", RoomAction.CHECK_CARD_TO_ATTACK);
                    putInvalidDataAndSendResponse(response, player);
                    return;
                };
                if (target.equals("hero")) {
                    boolean res = CardAttackUtil.checkCardToAttack(player, activePlayer, response,
                            allCards.get("field").get(pos), pos, target);
                    if (!res) {
                        response.put("status", "not_ok");
                        response.put("reason", "The card cannot attack right now");
                    }
                    else {
                        if (!allCards.get("field").get(pos).hasAction(CardRepository.Action.IGNORE_TAUNT)) {
                            List<Integer> positions = CardAttackUtil.checkTaunts(getPlayerData(getOtherPlayer(player)));
                            if (positions.size() > 0) {
                                response.put("status", "not_ok");
                                response.put("reason", "You must attack card with taunt");
                                res = false;
                            }
                        }
                        if (allCards.get("field").get(pos).hasStatus(CardRepository.Status.CAN_ATTACK_CARDS_ON_PLAY) && res) {
                            response.put("status", "not_ok");
                            response.put("reason", "Cards with board\ncannot attack heroes immediately");
                            res = false;
                        }
                        if (res) response.put("status", "ok");
                    }
                } else {
                    int opponentPos = msg.getInt("opponent_pos");
                    if (!checkCardIdFrom(msg, getPlayerData(getOtherPlayer(player)).getField(), opponentPos, true)) {
                        response.put("room_action", RoomAction.CHECK_CARD_TO_ATTACK);
                        putInvalidDataAndSendResponse(response, player);
                        return;
                    }
                    CardAttackUtil.checkCardToAttack(player, activePlayer, getPlayerData(getOtherPlayer(activePlayer)),
                            response, allCards.get("field").get(pos), pos, opponentPos, target);
                }
                if (response.getString("status").equals("not_ok")) sendResponse(response.toString(), player);
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

    private void putInvalidDataAndSendResponse(JSONObject response, GameServer.Client player) {
        response.put("status", "not_ok");
        response.put("reason", "There is something wrong with your client.\n Consider quitting");
        sendResponse(response.toString(), player);
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

        JSONObject attackerResponse = new JSONObject();
        JSONObject attackedResponse = new JSONObject();
        List<Integer> attackerIndexes = new ArrayList<>(List.of(attackerPos));
        List<Integer> attackedIndexes = new ArrayList<>(List.of(attackedPos));
        CardAttackUtil.checkAttackSpecialEffects(attacker, attacked, attackerIndexes, attackedIndexes,
                attackerResponse, attackedResponse, player, this);

        CardAttackUtil.putCardCardAttackAnimationInfo(attackerResponse, attackerPos, attackedPos, "attacker");
        CardUtil.putFieldChanges(attackerResponse, attackerField, attackedField, attackerIndexes, attackedIndexes);
        sendResponse(attackerResponse.toString(), player);

        CardAttackUtil.putCardCardAttackAnimationInfo(attackedResponse, attackedPos, attackerPos, "attacked");
        CardUtil.putFieldChanges(attackedResponse, attackedField, attackerField, attackedIndexes, attackerIndexes);
        sendResponse(attackedResponse.toString(), getOtherPlayer(player));

        CardUtil.removeDefeatedCards(attackerField);
        CardUtil.removeDefeatedCards(attackedField);
    }

    private void onCardHeroAttack(JSONObject msg, GameServer.Client player) {
        PlayerData playerData = getPlayerData(player);
        Card attacker = playerData.getField().get(msg.getInt("pos"));
        Hero attackedHero = getPlayerData(player).getHero();
        JSONObject responseAttacker = new JSONObject();
        JSONObject responseAttacked = new JSONObject();
        PlayerRoomUtil.onHeroAttacked(responseAttacker, responseAttacked, attackedHero, attacker, msg.getInt("pos"));

        sendResponse(responseAttacker.toString(), player);
        sendResponse(responseAttacked.toString(), getOtherPlayer(player));
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
                    JSONObject response = new JSONObject();
                    response.put("room_action", RoomAction.TIMER_UPDATE.toString());
                    response.put("status", String.valueOf(seconds));
                    response.put("maxSeconds", TIMER_MAX_SECONDS);
                    server.sendResponse(response.toString(), client);
                }
                if (!Thread.currentThread().isInterrupted()) {
                    JSONObject response = new JSONObject();
                    response.put("room_action", RoomAction.TIMER_UPDATE.toString());
                    response.put("status", "end");
                    server.sendResponse(response.toString(), client);
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
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.DRAW_CARD.toString());
        Card cardToDraw;

        PlayerData playerData = getPlayerData(player);
        try {
            cardToDraw = playerData.getDeck().remove(0);
            if (playerData.getHand().size() == HAND_SIZE) {
                response.put("card_status", "burned");
            } else {
                playerData.getHand().add(cardToDraw);
                response.put("card_status", "drawn");
            }
            CardUtil.putCardInfo(cardToDraw, response);
            CardUtil.checkProfessorDogOnCardDraw(playerData.getField(), player, getOtherPlayer(player), this);
        } catch (IndexOutOfBoundsException e) {
            response.put("card_status", "no_card");
            JSONObject responsePlayerDamaged = new JSONObject();
            JSONObject responsePlayerOther = new JSONObject();
            playerData.incrementBurntCardDamage();
            PlayerRoomUtil.dealDamageOnNoCard(playerData, responsePlayerDamaged, responsePlayerOther);
            sendResponse(responsePlayerDamaged.toString(), player);
            sendResponse(responsePlayerOther.toString(), getOtherPlayer(player));

            if (dataPlayer1.getHero().getHp() <= 0 || dataPlayer2.getHero().getHp() <= 0) {
                onPlayerDefeated();
            }
        }
        response.put("deck_size", playerData.getDeck().size());
        response.put("status", "ok");
        sendResponse(response.toString(), player);

        JSONObject opponentResponse = new JSONObject();
        opponentResponse.put("room_action", RoomAction.DRAW_CARD_OPPONENT.toString());
        opponentResponse.put("opponent_hand_size", playerData.getHand().size());
        sendResponse(opponentResponse.toString(), getOtherPlayer(player));
    }

    private void onPlayerDefeated() {
        JSONObject responseWinner = new JSONObject();
        JSONObject responseDefeated = new JSONObject();
        GameServer.Client winner;
        if (dataPlayer1.getHero().getHp() <= 0) winner = player2;
        else winner = player1;
        GameServer.Client defeated = getOtherPlayer(winner);
        PlayerRoomUtil.onHeroDefeated(responseWinner, responseDefeated, winner, defeated);
        end();
        sendResponses(responseWinner, responseDefeated, winner, defeated);
    }

    private void sendResponse(String response, GameServer.Client client) {
        try {
            client.getOutput().write(response);
            client.getOutput().newLine();
            client.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setActivePlayer() {
        if (random.nextBoolean()) activePlayer = player1;
        else activePlayer = player2;

        drawCard(activePlayer);

        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.BEGIN_TURN);
        response.put("status", "ok");
        launchTimer(activePlayer);

        Hero resHero =
                ManaUtil.increaseMana(response, getPlayerData(activePlayer));
        sendResponse(response.toString(), activePlayer);

        JSONObject responseOpponentMana = new JSONObject();
        ManaUtil.getOpponentMana(responseOpponentMana, resHero);
        sendResponse(responseOpponentMana.toString(), getNonActivePlayer());
    }

    private final int BACKGROUND_AMOUNT = 9;
    private void sendInitialInfo() {
        setInitialInfo(player1);
        setInitialInfo(player2);

        JSONObject player1Response = new JSONObject();
        JSONObject player2Response = new JSONObject();

        PlayerRoomUtil.putHpInfo(player1Response, dataPlayer1.getHero().getHp(), dataPlayer2.getHero().getHp());
        PlayerRoomUtil.putHpInfo(player2Response, dataPlayer2.getHero().getHp(), dataPlayer1.getHero().getHp());

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

    private final int INITIAL_HAND_SIZE = 4;

    private final int HAND_SIZE = 7;

    private void putInitialInfo(JSONObject response, JSONObject otherResponse,
                                List<Card> deck, GameServer.Client player, int background) {
        response.put("room_action", RoomAction.GET_INITIAL_INFO.toString());
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

        response.put("hand", arrayHand);
        response.put("deck_size", deck.size());
        response.put("background", "bg_" + background + ".png");

        otherResponse.put("opponent_hand_size", arrayHand.length());
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
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.GAME_END);
        response.put("result", "win");
        end();
        try {
            PlayerRoomUtil.updateUsers(response, getOtherPlayer(client), client);
        } catch (SQLException e) {}
        sendResponse(response.toString(), getOtherPlayer(client));
    }

    public void putRoomAction(JSONObject responsePlayer, JSONObject responseOtherPlayer, RoomAction action) {
        responsePlayer.put("room_action", action.toString());
        responseOtherPlayer.put("room_action", action.toString());
    }

    public void sendResponses(JSONObject responsePlayer, JSONObject responseOtherPlayer, GameServer.Client player,
                              GameServer.Client otherPlayer) {
        server.sendResponse(responsePlayer.toString(), player);
        server.sendResponse(responseOtherPlayer.toString(), otherPlayer);
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
