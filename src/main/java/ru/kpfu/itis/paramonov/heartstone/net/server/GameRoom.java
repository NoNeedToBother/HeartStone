package ru.kpfu.itis.paramonov.heartstone.net.server;

import ru.kpfu.itis.paramonov.heartstone.model.card.Card;

import java.util.ArrayList;
import java.util.List;

public class GameRoom {

    private GameServer.Client client1;

    private GameServer.Client client2;

    private List<Card> player1Hand = new ArrayList<>();

    private List<Card> player2Hand = new ArrayList<>();

    private List<Card> player1Deck = new ArrayList<>();

    private List<Card> player2Deck = new ArrayList<>();

    public void onStart() {
        //get decks, shuffle, draw hands, etc. requires database
    }



    public GameRoom(GameServer.Client client1, GameServer.Client client2) {
        this.client1 = client1;
        this.client2 = client2;
    }

    public void sendBattlefieldMessage(String message) {

    }

    public void sendHandMessage(GameServer.Client client) {

    }
}
