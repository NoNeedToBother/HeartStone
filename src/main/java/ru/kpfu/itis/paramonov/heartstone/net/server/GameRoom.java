package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameRoom {

    public enum RoomAction {
        GET_BACKGROUND;
    }

    private GameServer.Client client1;

    private GameServer.Client client2;

    private GameServer server;

    private List<Card> player1Hand = new ArrayList<>();

    private List<Card> player2Hand = new ArrayList<>();

    private List<Card> player1Deck = new ArrayList<>();

    private List<Card> player2Deck = new ArrayList<>();

    Random random = new Random();

    public GameRoom(GameServer.Client client1, GameServer.Client client2, GameServer server) {
        this.client1 = client1;
        this.client2 = client2;
        this.server = server;
    }

    public void onStart() {
        setBackground();
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
}
