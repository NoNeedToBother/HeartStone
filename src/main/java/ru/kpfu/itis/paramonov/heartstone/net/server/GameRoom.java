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
        GET_BACKGROUND, GET_BATTLE_DECK
    }

    private GameServer.Client client1;

    private GameServer.Client client2;

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

    public void onStart() {
        setBackground();
        sendGameDeck();
    }

    public void sendGameDeck() {
        player1Deck = getShuffledDeck(client1);
        player2Deck = getShuffledDeck(client2);
        sendDeck(player1Deck, client1);
        sendDeck(player2Deck, client2);
    }

    private void sendDeck(List<Card> deck, GameServer.Client client) {
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.GET_BATTLE_DECK.toString());
        JSONArray array = new JSONArray();
        for (Card card : deck) {
            putCardInfo(card, array);
        }
        response.put("cards", array);
        try {
            client.getOutput().write(response.toString());
            client.getOutput().newLine();
            client.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void putCardInfo(Card card, JSONArray responseCards) {
        JSONObject jsonCard = new JSONObject();
        jsonCard.put("atk", card.getAtk());
        jsonCard.put("hp", card.getHp());
        jsonCard.put("cost", card.getCost());
        CardRepository.CardTemplate cardInfo = card.getCardInfo();
        jsonCard.put("id", cardInfo.getId());

        responseCards.put(jsonCard);
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
