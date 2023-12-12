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
        GET_BACKGROUND, GET_BATTLE_DECK, DRAW_CARD, BEGIN_TURN, END_TURN
    }

    private GameServer.Client client1;

    private GameServer.Client client2;

    private GameServer.Client activePlayer;

    private GameServer server;

    private List<Card> player1Hand = new ArrayList<>();

    private List<Card> player2Hand = new ArrayList<>();

    private List<Card> player1Deck = new ArrayList<>();

    private List<Card> player2Deck = new ArrayList<>();

    private UserService service = new UserService();

    Random random = new Random();

    public GameRoom(GameServer.Client client1, GameServer.Client client2, GameServer server) {
        this.client1 = client1;
        this.client2 = client2;
        this.server = server;
    }

    public void handleMessage(JSONObject msg, GameServer.Client client) {
        switch (RoomAction.valueOf(msg.getString("room_action"))) {
            case END_TURN -> {
                if (client.equals(client1)) activePlayer = client2;
                else activePlayer = client1;
                JSONObject responseEnd = new JSONObject();
                responseEnd.put("room_action", RoomAction.END_TURN.toString());
                responseEnd.put("status", "ok");
                sendResponse(responseEnd.toString(), client);

                JSONObject responseBegin = new JSONObject();
                responseBegin.put("room_action", RoomAction.BEGIN_TURN.toString());
                responseBegin.put("status", "ok");

                drawCard(responseBegin, activePlayer);
                sendResponse(responseBegin.toString(), activePlayer);
            }
        }
    }

    private void drawCard(JSONObject response, GameServer.Client client) {
        JSONObject card = new JSONObject();
        JSONArray deck = new JSONArray();
        Card cardToDraw;
        if (client.equals(client1)) {
            cardToDraw = player1Deck.remove(0);
            putDeckInfo(player1Deck, deck);

        } else {
            cardToDraw = player2Deck.remove(0);
            putDeckInfo(player2Deck, deck);
        }
        putCardInfo(cardToDraw, response);
        response.put("deck", deck);
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

    public void onStart() {
        setActivePlayer();
        setBackground();
        sendGameDeck();
    }



    private void setActivePlayer() {
        if (random.nextBoolean()) activePlayer = client1;
        else activePlayer = client2;
    }

    public void sendGameDeck() {
        player1Deck = getShuffledDeck(client1);
        player2Deck = getShuffledDeck(client2);
        sendHandAndDeck(player1Deck, client1);
        sendHandAndDeck(player2Deck, client2);
    }

    private final int STANDARD_HAND_SIZE = 5;

    private void sendHandAndDeck(List<Card> deck, GameServer.Client client) {
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.GET_BATTLE_DECK.toString());
        JSONArray arrayHand = new JSONArray();
        JSONArray arrayDeck = new JSONArray();

        int handSize = STANDARD_HAND_SIZE;
        int counter = 1;

        for (Card card : deck) {
            if (counter <= handSize) putCardInfo(card, arrayHand);
            else putCardInfo(card, arrayDeck);
        }
        response.put("hand", arrayHand);
        response.put("deck", arrayDeck);

        sendResponse(response.toString(), client);
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
        server.sendResponse(json.toString(), client1);
        server.sendResponse(json.toString(), client2);
    }

    public void sendHandMessage(GameServer.Client client) {

    }

    public GameServer.Client getClient1() {
        return client1;
    }

    public GameServer.Client getClient2() {
        return client2;
    }
}
