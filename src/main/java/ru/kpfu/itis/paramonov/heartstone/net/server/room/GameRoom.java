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

    private Hero player1Hero;

    private int burntCardDmgPlayer1 = 0;

    private Hero player2Hero;

    private int burntCardDmgPlayer2 = 0;

    private GameServer.Client activePlayer;

    private final GameServer server;

    HashMap<String, List<Card>> player1AllCards = setPlayerCardMap();

    HashMap<String, List<Card>> player2AllCards = setPlayerCardMap();

    private Thread player1Timer;

    private Thread player2Timer;

    Random random = new Random();

    public GameRoom(GameServer.Client player1, GameServer.Client player2, GameServer server) {
        this.player1 = player1;
        this.player2 = player2;
        this.server = server;
    }

    public void onStart() {
        sendInitialInfo();
        setActivePlayer();
    }

    private HashMap<String, List<Card>> setPlayerCardMap() {
        HashMap<String, List<Card>> res = new HashMap<>();
        res.put("field", new ArrayList<>());
        res.put("hand", new ArrayList<>());
        res.put("deck", new ArrayList<>());
        res.put("played", new ArrayList<>());
        res.put("defeated", new ArrayList<>());
        return res;
    }

    public GameServer.Client getNonActivePlayer() {
        if (activePlayer.equals(player1)) return player2;
        else return player1;
    }
    public GameServer.Client getOtherPlayer(GameServer.Client player) {
        if (player.equals(player1)) return player2;
        else return player1;
    }
    public HashMap<String, List<Card>> getAllCards(GameServer.Client player) {
        if (player.equals(player1)) return player1AllCards;
        else return player2AllCards;
    }
    private Hero getHero(GameServer.Client player) {
        if (player.equals(player1)) return player1Hero;
        else return player2Hero;
    }

    public void handleMessage(JSONObject msg, GameServer.Client client) {
        switch (RoomAction.valueOf(msg.getString("room_action"))) {
            case END_TURN -> {
                if (client.equals(player1)) {
                    player1Timer.interrupt();
                    activePlayer = player2;
                    CardUtil.changeCardAbilityToAttackOnTurnEnd(player1AllCards.get("field"), player1, player2, server);
                }
                else {
                    player2Timer.interrupt();
                    activePlayer = player1;
                    CardUtil.changeCardAbilityToAttackOnTurnEnd(player2AllCards.get("field"), player2, player1, server);
                }
                CardUtil.checkEndTurnCards(client, server, this);
                JSONObject responseEnd = new JSONObject();
                responseEnd.put("room_action", RoomAction.END_TURN.toString());
                responseEnd.put("status", "ok");
                sendResponse(responseEnd.toString(), client);

                JSONObject responseBegin = new JSONObject();
                responseBegin.put("room_action", RoomAction.BEGIN_TURN.toString());
                responseBegin.put("status", "ok");

                Hero resHero =
                        ManaUtil.increaseMana(responseBegin, activePlayer, player1, player1Hero, player2Hero);
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
                if (client != activePlayer) {
                    response.put("status", "ok");
                    response.put("reason", "Not your turn");
                    sendResponse(response.toString(), client);
                }
                int mana = getHero(client).getMana();

                CardOnPlayedUtil.checkCardPlayed(response, client, activePlayer, player1, msg.getInt("hand_pos"),
                        player1AllCards, player2AllCards, mana);
                try {
                    String action = msg.getString("card_action");
                    response.put("card_action", msg.getString("card_action"));
                    switch (action) {
                        case "target_enemy_card" -> response.put("opponent_pos", msg.getInt("opponent_pos"));
                    }
                } catch (JSONException ignored) {}
                List<CardRepository.Status> initialStatuses = new ArrayList<>();
                Card card = getAllCards(client).get("hand").get(msg.getInt("hand_pos"));
                if (card.hasAction(CardRepository.Action.SHIELD_ON_PLAY)) {
                    initialStatuses.add(CardRepository.Status.SHIELDED);
                }
                CardUtil.putStatuses(initialStatuses, response);
                sendResponse(response.toString(), client);
            }

            case PLAY_CARD -> {
                JSONObject opponentResponse = new JSONObject();
                Card playedCard = CardOnPlayedUtil.onCardPlayed(msg, client, player1, player1AllCards, player2AllCards);
                List<CardRepository.Status> initialStatuses = new ArrayList<>();
                if (playedCard.hasAction(CardRepository.Action.CANNOT_ATTACK_ON_PLAY))
                    playedCard.addStatus(CardRepository.Status.CANNOT_ATTACK);
                if (playedCard.hasAction(CardRepository.Action.SHIELD_ON_PLAY)) {
                    playedCard.addStatus(CardRepository.Status.SHIELDED);
                    initialStatuses.add(CardRepository.Status.SHIELDED);
                }
                if (client.equals(player1)) player1AllCards.get("played").add(playedCard);
                else player2AllCards.get("played").add(playedCard);
                Hero hero = getHero(client);
                int newMana = hero.getMana() - playedCard.getCost();
                hero.setMana(newMana);
                opponentResponse.put("room_action", RoomAction.PLAY_CARD_OPPONENT.toString());
                opponentResponse.put("status", "ok");
                CardUtil.putCardStatsAndId(playedCard, opponentResponse);
                CardUtil.putStatuses(initialStatuses, opponentResponse);
                opponentResponse.put("opponent_mana", newMana);
                opponentResponse.put("opponent_hand_size", getAllCards(client).get("hand").size());
                sendResponse(opponentResponse.toString(), getOtherPlayer(client));

                JSONObject response = new JSONObject();
                response.put("room_action", RoomAction.PLAY_CARD.toString());
                response.put("pos", msg.getInt("pos"));
                CardUtil.putStatuses(initialStatuses, response);
                response.put("mana", newMana);
                sendResponse(response.toString(), client);

                CardOnPlayedUtil.checkOnCardPlayed(client, getAllCards(client), playedCard, server);

                if (playedCard.hasKeyWord(CardRepository.KeyWord.BATTLE_CRY)) {
                    try {
                        msg.getString("card_action");
                        JSONObject responsePlayer1 = new JSONObject();
                        JSONObject responsePlayer2 = new JSONObject();
                        CardOnPlayedUtil.checkBattleCry(client, player1, player2, player1AllCards, player2AllCards,
                                playedCard, msg, responsePlayer1, responsePlayer2, server, this);
                    } catch (JSONException ignored) {}
                }
            }

            case CHECK_CARD_TO_ATTACK -> {
                JSONObject response = new JSONObject();
                HashMap<String, List<Card>> allCards = getAllCards(client);
                String target = msg.getString("target");
                int pos = msg.getInt("pos");
                if(client != activePlayer) {
                    response.put("status", "ok");
                    response.put("reason", "Not your turn");
                    sendResponse(response.toString(), client);
                }
                if (target.equals("hero")) {
                    boolean res = CardAttackUtil.checkCardToAttack(client, activePlayer, getAllCards(getOtherPlayer(activePlayer)),
                            response, allCards.get("field").get(pos), pos, target);
                    if (!res) {
                        response.put("status", "not_ok");
                        response.put("reason", "The card cannot attack right now");
                    }
                    else {
                        if (!allCards.get("field").get(pos).hasAction(CardRepository.Action.IGNORE_TAUNT)) {
                            List<Integer> positions = CardAttackUtil.checkTaunts(getAllCards(getOtherPlayer(client)));
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
                    CardAttackUtil.checkCardToAttack(client, activePlayer, getAllCards(getOtherPlayer(activePlayer)),
                            response, allCards.get("field").get(pos), pos, opponentPos, target);
                }
                if (response.getString("status").equals("not_ok")) sendResponse(response.toString(), client);
                else {
                    if (target.equals("hero")) onCardHeroAttack(msg, client);
                    else onCardCardAttack(msg, client);
                }
            }
        }
    }

    private void onCardCardAttack(JSONObject msg, GameServer.Client client) {
        GameServer.Client otherPlayer = getOtherPlayer(client);
        Hero attackerHero = getHero(client);
        Hero attackedHero = getHero(otherPlayer);
        List<Card> attackerField = getAllCards(client).get("field");
        List<Card> attackedField = getAllCards(otherPlayer).get("field");
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
        CardAttackUtil.checkAttackSpecialEffects(attacker, attacked, attackerField, attackedField, attackerIndexes, attackedIndexes,
                attackerHero, attackedHero, attackerResponse, attackedResponse, client, getOtherPlayer(client), this);

        CardAttackUtil.putCardCardAttackAnimationInfo(attackerResponse, attackerPos, attackedPos, "attacker");
        CardUtil.putFieldChanges(attackerResponse, attackerField, attackedField, attackerIndexes, attackedIndexes);
        sendResponse(attackerResponse.toString(), client);

        CardAttackUtil.putCardCardAttackAnimationInfo(attackedResponse, attackedPos, attackerPos, "attacked");
        CardUtil.putFieldChanges(attackedResponse, attackedField, attackerField, attackedIndexes, attackerIndexes);
        sendResponse(attackedResponse.toString(), getOtherPlayer(client));

        CardUtil.removeDefeatedCards(attackerField);
        CardUtil.removeDefeatedCards(attackedField);
    }

    private void onCardHeroAttack(JSONObject msg, GameServer.Client client) {
        HashMap<String, List<Card>> allCards = getAllCards(client);
        Card attacker = allCards.get("field").get(msg.getInt("pos"));
        Hero attackedHero;
        if (client.equals(player1)) attackedHero = player2Hero;
        else attackedHero = player1Hero;
        JSONObject responseAttacker = new JSONObject();
        JSONObject responseAttacked = new JSONObject();
        PlayerRoomUtil.onHeroAttacked(responseAttacker, responseAttacked, attackedHero, attacker, msg.getInt("pos"));

        sendResponse(responseAttacker.toString(), client);
        sendResponse(responseAttacked.toString(), getOtherPlayer(client));
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

    public void drawCard(GameServer.Client client) {
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.DRAW_CARD.toString());
        Card cardToDraw;

        Map<String, List<Card>> allCards = getAllCards(client);
        try {
            cardToDraw = allCards.get("deck").remove(0);
            if (allCards.get("hand").size() == HAND_SIZE) {
                response.put("card_status", "burned");
            } else {
                allCards.get("hand").add(cardToDraw);
                response.put("card_status", "drawn");
            }
            CardUtil.putCardInfo(cardToDraw, response);
            CardUtil.checkProfessorDogOnCardDraw(allCards.get("field"), client, getOtherPlayer(client), server);
        } catch (IndexOutOfBoundsException e) {
            response.put("card_status", "no_card");
            JSONObject responsePlayer1 = new JSONObject();
            JSONObject responsePlayer2 = new JSONObject();
            if (client.equals(player1)) burntCardDmgPlayer1++;
            else burntCardDmgPlayer2++;
            PlayerRoomUtil.dealDamageOnNoCard(client, player1, burntCardDmgPlayer1, burntCardDmgPlayer2, player1Hero,
                    player2Hero, responsePlayer1, responsePlayer2);
            sendResponse(responsePlayer1.toString(), player1);
            sendResponse(responsePlayer2.toString(), player2);

            if (player1Hero.getHp() <= 0 || player2Hero.getHp() <= 0) {
                onPlayerDefeated();
            }
        }
        response.put("deck_size", allCards.get("deck").size());
        response.put("status", "ok");
        sendResponse(response.toString(), client);

        JSONObject opponentResponse = new JSONObject();
        opponentResponse.put("room_action", RoomAction.DRAW_CARD_OPPONENT.toString());
        opponentResponse.put("opponent_hand_size", allCards.get("hand").size());
        sendResponse(opponentResponse.toString(), getOtherPlayer(client));
    }

    private void onPlayerDefeated() {
        JSONObject responseWinner = new JSONObject();
        JSONObject responseDefeated = new JSONObject();
        GameServer.Client winner;
        if (player1Hero.getHp() <= 0) winner = player2;
        else winner = player1;
        GameServer.Client defeated = getOtherPlayer(winner);
        PlayerRoomUtil.onHeroDefeated(responseWinner, responseDefeated, winner, defeated);
        end();
        sendResponse(responseWinner.toString(), winner);
        sendResponse(responseDefeated.toString(), defeated);
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
                ManaUtil.increaseMana(response, activePlayer, player1, player1Hero, player2Hero);
        sendResponse(response.toString(), activePlayer);

        JSONObject responseOpponentMana = new JSONObject();
        ManaUtil.getOpponentMana(responseOpponentMana, resHero);
        sendResponse(responseOpponentMana.toString(), getNonActivePlayer());
    }

    private final int BACKGROUND_AMOUNT = 9;
    private void sendInitialInfo() {
        player1AllCards.put("deck", getShuffledDeck(player1));
        player2AllCards.put("deck", getShuffledDeck(player2));
        CardUtil.makeCardsUnableToAttackOnStart(player1AllCards.get("deck"));
        CardUtil.makeCardsUnableToAttackOnStart(player2AllCards.get("deck"));

        int player1Hp = PlayerRoomUtil.getInitialHp(player1AllCards.get("deck"));
        int player2Hp = PlayerRoomUtil.getInitialHp(player2AllCards.get("deck"));
        player1Hero = new Hero(player1Hp, player1Hp, 0, 0);
        player2Hero = new Hero(player2Hp, player2Hp, 0, 0);

        JSONObject player1Response = new JSONObject();
        JSONObject player2Response = new JSONObject();
        PlayerRoomUtil.putHpInfo(player1Response, player1Hp, player2Hp);
        PlayerRoomUtil.putHpInfo(player2Response, player2Hp, player1Hp);

        int randomBg = random.nextInt(1, BACKGROUND_AMOUNT + 1);
        putInitialInfo(player1Response, player2Response, player1AllCards.get("deck"), player1, randomBg);
        putInitialInfo(player2Response, player1Response, player2AllCards.get("deck"), player2, randomBg);

        sendResponse(player1Response.toString(), player1);
        sendResponse(player2Response.toString(), player2);
    }

    private final int INITIAL_HAND_SIZE = 4;

    private final int HAND_SIZE = 7;

    private void putInitialInfo(JSONObject response, JSONObject otherResponse,
                                List<Card> deck, GameServer.Client client, int background) {
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
        getAllCards(client).put("hand", handCards);

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

    private void end() {
        player1.notifyGameEnd();
        player2.notifyGameEnd();
        player1Hero = null;
        player2Hero = null;
        player1AllCards = null;
        player2AllCards = null;
        try {
            player1Timer.interrupt();
        } catch (NullPointerException ignored) {}
        try {
            player2Timer.interrupt();
        } catch (NullPointerException ignored) {}
        activePlayer = null;
    }
}
