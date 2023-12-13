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
        GET_BACKGROUND, GET_BATTLE_DECK, DRAW_CARD, BEGIN_TURN, END_TURN, PLAY_CARD
    }

    private GameServer.Client player1;

    private GameServer.Client player2;

    private GameServer.Client activePlayer;

    private GameServer server;

    /*

    private List<Card> player1FieldCards = new ArrayList<>();

    private List<Card> player2FieldCards = new ArrayList<>();

    private List<Card> player1Hand = new ArrayList<>();

    private List<Card> player2Hand = new ArrayList<>();

    private List<Card> player1Deck = new ArrayList<>();

    private List<Card> player2Deck = new ArrayList<>();*/

    HashMap<String, List<Card>> player1AllCards = getPlayersMaps();

    HashMap<String, List<Card>> player2AllCards = getPlayersMaps();

    private UserService service = new UserService();

    Random random = new Random();

    public GameRoom(GameServer.Client player1, GameServer.Client player2, GameServer server) {
        this.player1 = player1;
        this.player2 = player2;
        this.server = server;
    }

    private HashMap<String, List<Card>> getPlayersMaps() {
        HashMap<String, List<Card>> res = new HashMap<>();
        res.put("field", new ArrayList<>());
        res.put("hand", new ArrayList<>());
        res.put("deck", new ArrayList<>());
        return res;
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

                drawCard(responseBegin, activePlayer);
                sendResponse(responseBegin.toString(), activePlayer);
            }
        }
    }

    private void drawCard(JSONObject response, GameServer.Client client) {
        JSONArray deck = new JSONArray();
        Card cardToDraw;

        Map<String, List<Card>> allCards;
        if (client.equals(player1)) allCards = player1AllCards;
        else allCards = player2AllCards;

        cardToDraw = allCards.get("deck").remove(0);
        putDeckInfo(allCards.get("deck"), deck);
        if (allCards.get("hand").size() == HAND_SIZE) {
            response.put("card_status", "burned");
        }
        else {
            allCards.get("hand").add(cardToDraw);
            response.put("card_status", "drawn");
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
        sendGameHandAndDeck();
    }



    private void setActivePlayer() {
        if (random.nextBoolean()) activePlayer = player1;
        else activePlayer = player2;

        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.BEGIN_TURN);
        response.put("status", "ok");
        sendResponse(response.toString(), activePlayer);
    }

    public void sendGameHandAndDeck() {
        player1AllCards.put("deck", getShuffledDeck(player1));
        player2AllCards.put("deck", getShuffledDeck(player2));
        sendHandAndDeck(player1AllCards.get("deck"), player1);
        sendHandAndDeck(player2AllCards.get("deck"), player2);
        /*
        player1Deck = getShuffledDeck(player1);
        player2Deck = getShuffledDeck(player2);
        sendHandAndDeck(player1Deck, player1);
        sendHandAndDeck(player2Deck, player2);*/
    }

    private final int INITIAL_HAND_SIZE = 4;

    private final int HAND_SIZE = 6;

    private void sendHandAndDeck(List<Card> deck, GameServer.Client client) {
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.GET_BATTLE_DECK.toString());
        JSONArray arrayHand = new JSONArray();
        JSONArray arrayDeck = new JSONArray();

        int handSize = INITIAL_HAND_SIZE;
        int counter = 1;

        for (Card card : deck) {
            if (counter <= handSize) putCardInfo(card, arrayHand);
            else putCardInfo(card, arrayDeck);
            counter++;
        }
        response.put("hand", arrayHand);
        response.put("deck", arrayDeck);

        if (client.equals(activePlayer)) drawCard(response, client);

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
        server.sendResponse(json.toString(), player1);
        server.sendResponse(json.toString(), player2);
    }

    public GameServer.Client getPlayer1() {
        return player1;
    }

    public GameServer.Client getPlayer2() {
        return player2;
    }


}
