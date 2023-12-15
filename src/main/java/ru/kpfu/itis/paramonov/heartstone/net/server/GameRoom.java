package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GameRoom {

    public enum RoomAction {
        GET_BACKGROUND, GET_HAND_AND_DECK, DRAW_CARD, BEGIN_TURN, END_TURN, PLAY_CARD, PLAY_CARD_OPPONENT, CARD_CARD_ATTACK,
        GET_OPPONENT_MANA, CHECK_CARD_PLAYED
    }

    private GameServer.Client player1;

    private GameServer.Client player2;

    private HashMap<String, Integer> player1Mana = setPlayerManaMap();

    private HashMap<String, Integer> player2Mana = setPlayerManaMap();

    private GameServer.Client activePlayer;

    private GameServer server;

    HashMap<String, List<Card>> player1AllCards = setPlayerCardMap();

    HashMap<String, List<Card>> player2AllCards = setPlayerCardMap();

    private UserService service = new UserService();

    Random random = new Random();

    public GameRoom(GameServer.Client player1, GameServer.Client player2, GameServer server) {
        this.player1 = player1;
        this.player2 = player2;
        this.server = server;
    }

    public void onStart() {
        setActivePlayer();
        setBackground();
        sendGameHandAndDeck();
    }

    private HashMap<String, List<Card>> setPlayerCardMap() {
        HashMap<String, List<Card>> res = new HashMap<>();
        res.put("field", Collections.synchronizedList(new ArrayList<>()));
        res.put("hand", Collections.synchronizedList(new ArrayList<>()));
        res.put("deck", Collections.synchronizedList(new ArrayList<>()));
        return res;
    }

    private HashMap<String, Integer> setPlayerManaMap() {
        return new HashMap<>(Map.of("mana", 0, "maxMana", 0));
    }

    public GameServer.Client getNonActivePlayer() {
        if (activePlayer.equals(player1)) return player2;
        else return player1;
    }

    public GameServer.Client getOtherPlayer(GameServer.Client player) {
        if (player.equals(player1)) return player2;
        else return player1;
    }

    public void handleMessage(JSONObject msg, GameServer.Client client) {
        switch (RoomAction.valueOf(msg.getString("room_action"))) {
            case END_TURN -> {
                if (client.equals(player1)) activePlayer = player2;
                else activePlayer = player1;
                JSONObject responseEnd = new JSONObject();
                responseEnd.put("room_action", RoomAction.END_TURN.toString());
                responseEnd.put("status", "ok");
                sendResponse(responseEnd.toString(), client);

                JSONObject responseBegin = new JSONObject();
                responseBegin.put("room_action", RoomAction.BEGIN_TURN.toString());
                responseBegin.put("status", "ok");

                HashMap<String, Integer> resMana =
                        ManaHelper.increaseMana(responseBegin, activePlayer, player1, player1Mana, player2Mana);
                sendResponse(responseBegin.toString(), activePlayer);

                JSONObject responseOpponentMana = new JSONObject();
                ManaHelper.getOpponentMana(responseOpponentMana, resMana);
                sendResponse(responseOpponentMana.toString(), getNonActivePlayer());
                drawCard(activePlayer);
            }

            case CHECK_CARD_PLAYED -> {
                JSONObject response = new JSONObject();
                response.put("room_action", RoomAction.CHECK_CARD_PLAYED.toString());
                int mana;
                if (client.equals(player1)) mana = player1Mana.get("mana");
                else mana = player2Mana.get("mana");

                PlayedCardHelper.checkCardPlayed(response, client, player1, Integer.parseInt(msg.getString("hand_pos")),
                        player1AllCards, player2AllCards, mana);

                sendResponse(response.toString(), client);
            }

            case PLAY_CARD -> {
                Card playedCard = PlayedCardHelper.onCardPlayed(msg, client, player1, player1AllCards, player2AllCards);

                JSONObject response = new JSONObject();
                PlayedCardHelper.putPlayedCardForOpponent(response, playedCard);
                sendResponse(response.toString(), getOtherPlayer(client));
            }

            case CARD_CARD_ATTACK -> {
                List<Card> attackerField;
                List<Card> attackedField;
                if (client.equals(player1)) {
                    attackerField = player1AllCards.get("field");
                    attackedField = player2AllCards.get("field");
                }
                else {
                    attackerField = player2AllCards.get("field");
                    attackedField = player1AllCards.get("field");
                }

                Card attacker = attackerField.get(Integer.parseInt(msg.getString("attacker_pos")));
                Card attacked = attackedField.get(Integer.parseInt(msg.getString("attacked_pos")));
                attacked.decreaseHp(attacker.getAtk());
                attacker.decreaseHp(attacked.getAtk());

                JSONObject attackerResponse = new JSONObject();
                attackerResponse.put("room_action", RoomAction.CARD_CARD_ATTACK.toString());
                attackerResponse.put("status", "ok");
                putFieldChanges(attackerResponse, attackerField, Integer.parseInt(msg.getString("attacker_pos")));
                putOpponentChanges(attackerResponse, attackedField, Integer.parseInt(msg.getString("attacked_pos")));
                sendResponse(attackerResponse.toString(), client);

                JSONObject attackedResponse = new JSONObject();
                attackedResponse.put("room_action", RoomAction.CARD_CARD_ATTACK.toString());
                attackedResponse.put("status", "ok");
                putFieldChanges(attackedResponse, attackedField, Integer.parseInt(msg.getString("attacked_pos")));
                putOpponentChanges(attackedResponse, attackerField, Integer.parseInt(msg.getString("attacker_pos")));
                sendResponse(attackedResponse.toString(), getOtherPlayer(client));

                if (attacker.getHp() <= 0) attackerField.remove(attacker);
                if (attacked.getHp() <= 0) attackedField.remove(attacked);
            }
        }
    }

    private void putFieldChanges(JSONObject response, List<Card> field, int... positions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("stat_changes", changes);
    }

    private JSONArray getFieldChanges(List<Card> field, int... positions) {
        JSONArray changes = new JSONArray();
        for (int pos = 0; pos < positions.length; pos++) {
            JSONObject changedCard = new JSONObject();
            System.out.println(field.get(pos).getCardInfo().getName());
            changedCard.put("pos", positions[pos]);
            changedCard.put("hp", field.get(positions[pos]).getHp());
            changedCard.put("atk", field.get(positions[pos]).getAtk());
            changes.put(changedCard);
        }
        return changes;
    }

    private void putOpponentChanges(JSONObject response, List<Card> field, int... positions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("opponent_stat_changes", changes);
    }

    private void drawCard(GameServer.Client client) {
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.DRAW_CARD.toString());
        JSONArray deck = new JSONArray();
        Card cardToDraw;

        Map<String, List<Card>> allCards;
        if (client.equals(player1)) allCards = player1AllCards;
        else allCards = player2AllCards;
        try {
            cardToDraw = allCards.get("deck").remove(0);
            putDeckInfo(allCards.get("deck"), deck);
            if (allCards.get("hand").size() == HAND_SIZE) {
                response.put("card_status", "burned");
            } else {
                allCards.get("hand").add(cardToDraw);
                response.put("card_status", "drawn");
            }
            putCardInfo(cardToDraw, response);
        } catch (IndexOutOfBoundsException e) {
            response.put("card_status", "no_card");
        }
        response.put("deck", deck);
        response.put("status", "ok");

        sendResponse(response.toString(), client);
    }


    private void putDeckInfo(List<Card> deck, JSONArray deckArray) {
        for (Card card : deck) {
            putCardInfo(card, deckArray);
        }
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

        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.BEGIN_TURN);
        response.put("status", "ok");

        HashMap<String, Integer> resMana =
                ManaHelper.increaseMana(response, activePlayer, player1, player1Mana, player2Mana);
        sendResponse(response.toString(), activePlayer);

        JSONObject responseOpponentMana = new JSONObject();
        ManaHelper.getOpponentMana(responseOpponentMana, resMana);
        sendResponse(responseOpponentMana.toString(), getNonActivePlayer());
    }

    public void sendGameHandAndDeck() {
        player1AllCards.put("deck", getShuffledDeck(player1));
        player2AllCards.put("deck", getShuffledDeck(player2));

        sendHandAndDeck(player1AllCards.get("deck"), player1);
        sendHandAndDeck(player2AllCards.get("deck"), player2);
    }

    private final int INITIAL_HAND_SIZE = 4;

    private final int HAND_SIZE = 6;

    private void sendHandAndDeck(List<Card> deck, GameServer.Client client) {
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.GET_HAND_AND_DECK.toString());
        JSONArray arrayHand = new JSONArray();
        JSONArray arrayDeck = new JSONArray();

        int counter = 1;

        List<Card> handCards = new ArrayList<>();

        for (Card card : deck) {
            if (counter <= INITIAL_HAND_SIZE) {
                handCards.add(card);
                putCardInfo(card, arrayHand);
            }
            else putCardInfo(card, arrayDeck);
            counter++;
        }
        deck.removeAll(handCards);
        if (client.equals(player1)) player1AllCards.put("hand", handCards);
        else player2AllCards.put("hand", handCards);

        response.put("hand", arrayHand);
        response.put("deck", arrayDeck);

        sendResponse(response.toString(), client);

        if (client.equals(activePlayer)) drawCard(client);
    }

    private void putCardInfo(Card card, JSONArray responseCards) {
        JSONObject jsonCard = getJsonCard(card);

        responseCards.put(jsonCard);
    }

    private void putCardInfo(Card card, JSONObject response) {
        JSONObject jsonCard = getJsonCard(card);

        response.put("card", jsonCard);
    }

    private JSONObject getJsonCard(Card card) {
        JSONObject jsonCard = new JSONObject();
        jsonCard.put("atk", card.getAtk());
        jsonCard.put("hp", card.getHp());
        jsonCard.put("cost", card.getCost());
        CardRepository.CardTemplate cardInfo = card.getCardInfo();
        jsonCard.put("id", cardInfo.getId());
        return jsonCard;
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
        String clientLogin = client.getUserLogin();
        String clientCardIdsString = service.get(clientLogin).getDeck();

        return clientCardIdsString;
    }

    private final int BACKGROUND_AMOUNT = 4;

    private void setBackground() {
        JSONObject json = new JSONObject();
        json.put("room_action", RoomAction.GET_BACKGROUND.toString());
        int randomBg = random.nextInt(1, BACKGROUND_AMOUNT + 1);
        json.put("status", "OK");
        json.put("background", "bg_" + randomBg + ".png");
        server.sendResponse(json.toString(), player1);
        server.sendResponse(json.toString(), player2);
    }
}
